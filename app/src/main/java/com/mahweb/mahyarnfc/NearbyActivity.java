package com.mahweb.mahyarnfc;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.*;

import com.mahweb.mahyarnfc.omnishare.*;
import com.mahweb.mahyarnfc.omnishare.ble.*;
import com.mahweb.mahyarnfc.omnishare.crypto.CryptoBox;
import com.mahweb.mahyarnfc.omnishare.identity.*;
import com.mahweb.mahyarnfc.omnishare.lan.*;
import com.mahweb.mahyarnfc.omnishare.nearby.*;
import com.mahweb.mahyarnfc.omnishare.transfer.TransferLedger;
import com.mahweb.mahyarnfc.omnishare.transport.*;
import com.mahweb.mahyarnfc.omnishare.trust.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class NearbyActivity extends Activity implements BleScanner.Listener, LanDiscoveryManager.Listener {
    private TextView scanState, empty;
    private LinearLayout list;
    private Button send;
    private final Map<String, CheckBox> boxes = new LinkedHashMap<>();
    private final Map<String, LanPeer> lanPeers = new ConcurrentHashMap<>();
    private final NearbyDeviceRegistry bleRegistry = new NearbyDeviceRegistry();
    private IdentityStore identity;
    private TrustRepository trust;
    private TransferLedger ledger;
    private BleScanner bleScanner;
    private LanServer lanServer;
    private LanDiscoveryManager lanDiscovery;
    private LanTransport lanTransport;
    private ExecutorService sendPool;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_nearby);
        scanState=findViewById(R.id.nearbyScanState); empty=findViewById(R.id.nearbyEmpty); list=findViewById(R.id.nearbyList); send=findViewById(R.id.btnSendSelected);
        findViewById(R.id.btnNearbyClose).setOnClickListener(v->finish());
        identity=new AndroidKeystoreIdentityStore(this); trust=new TrustRepository(this); ledger=new TransferLedger(); sendPool=Executors.newFixedThreadPool(4);
        send.setOnClickListener(v->sendSelected());
        startLan();
        startBleWhenAllowed();
    }

    private void startLan() {
        try {
            lanServer=new LanServer(identity,trust,ledger,new LanReceivePolicy(){
                public boolean approve(String deviceId,String fingerprint,String verificationCode,TransferEnvelope envelope){return approveUnknownBlocking(deviceId,fingerprint,verificationCode,envelope);}
                public void onReceived(String deviceId,String fingerprint,TransferEnvelope envelope){showIncoming(envelope);}
                public void onRejected(String deviceId,String fingerprint,String reason){runOnUiThread(()->scanState.setText("درخواست رد شد: "+reason));}
            });
            int port=lanServer.start();
            byte[] secret=DiscoverySecretStore.get(this);
            HmacEphemeralTokenProvider tokens=new HmacEphemeralTokenProvider(secret);
            lanDiscovery=new LanDiscoveryManager(this,tokens,BleProtocol.CAP_LAN|BleProtocol.CAP_GATT,this);
            lanDiscovery.advertise(port); lanDiscovery.discover();
            lanTransport=new LanTransport(identity,lanDiscovery,this::approveOutgoingPeerBlocking);
            scanState.setText("LAN فعال است؛ در حال جستجوی Mahyar روی همین Wi‑Fi...");
        } catch(Exception e) { scanState.setText("LAN آماده نشد: "+e.getMessage()); }
    }

    private void startBleWhenAllowed() {
        if (!BlePermissions.has(this)) { requestPermissions(BlePermissions.needed(this),BlePermissions.REQUEST_CODE); return; }
        bleScanner=new BleScanner(this,this); bleScanner.start();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] p,int[] r){super.onRequestPermissionsResult(requestCode,p,r);if(requestCode==BlePermissions.REQUEST_CODE&&BlePermissions.has(this))startBleWhenAllowed();}

    public void onObservation(BleObservation o) {
        NearbyDeviceRegistry.Entry e=bleRegistry.observe(o);
        runOnUiThread(()->upsertBle(e));
    }
    public void onError(int code,String message){runOnUiThread(()->scanState.setText("BLE: "+message));}
    public void onPeer(LanPeer peer){lanPeers.put(peer.serviceName,peer);runOnUiThread(()->upsertLan(peer));}
    public void onPeerLost(String serviceName){lanPeers.remove(serviceName);runOnUiThread(()->removeRow("lan:"+serviceName));}
    public void onError(String code,String message){runOnUiThread(()->scanState.setText(code+": "+message));}

    private void upsertLan(LanPeer p) {
        String key="lan:"+p.serviceName; CheckBox c=boxes.get(key);
        String label="📶 Wi‑Fi  "+p.serviceName+"  •  آماده ارسال";
        if(c==null){c=new CheckBox(this);c.setTag(key);list.addView(c);boxes.put(key,c);} c.setText(label); c.setEnabled(true); refreshEmpty();
    }
    private void upsertBle(NearbyDeviceRegistry.Entry e) {
        String key="ble:"+e.tokenHex; CheckBox c=boxes.get(key);
        String label="📡 BLE  "+e.alias+"  •  "+proximityFa(e.proximity());
        if(c==null){c=new CheckBox(this);c.setTag(key);list.addView(c);boxes.put(key,c);} c.setText(label); c.setEnabled(false); refreshEmpty();
    }
    private void removeRow(String key){CheckBox c=boxes.remove(key);if(c!=null)list.removeView(c);refreshEmpty();}
    private void refreshEmpty(){empty.setVisibility(boxes.isEmpty()?View.VISIBLE:View.GONE);}
    private static String proximityFa(ProximityBucket b){switch(b){case VERY_CLOSE:return "خیلی نزدیک";case NEAR:return "نزدیک";case MID:return "متوسط";case FAR:return "دور";default:return "در حال اندازه‌گیری";}}

    private void sendSelected() {
        List<Recipient> recipients=new ArrayList<>();
        for(Map.Entry<String,CheckBox> it:boxes.entrySet()) if(it.getValue().isChecked()&&it.getKey().startsWith("lan:")) {
            LanPeer p=lanPeers.get(it.getKey().substring(4)); if(p==null)continue;
            Map<String,String> m=new HashMap<>();m.put("serviceName",p.serviceName);m.put("host",p.host.getHostAddress());m.put("port",Integer.toString(p.port));m.put("token",p.tokenHex);
            recipients.add(new Recipient("","",p.serviceName,RelationshipState.UNKNOWN,EnumSet.of(TransportKind.LAN),m));
        }
        if(recipients.isEmpty()){Toast.makeText(this,"حداقل یک دستگاه Wi‑Fi را انتخاب کنید",Toast.LENGTH_SHORT).show();return;}
        Profile profile=ProfileRepository.load(this); byte[] payload;
        try{payload=profile.toJson().toString().getBytes(StandardCharsets.UTF_8);}catch(Exception e){Toast.makeText(this,"ساخت کارت ناموفق بود",Toast.LENGTH_SHORT).show();return;}
        long now=System.currentTimeMillis();
        TransferEnvelope e=TransferEnvelope.unsigned(UUID.randomUUID().toString(),identity.getOrCreate().deviceId,"",now,now+120_000L,PayloadType.PROFILE_JSON,CryptoBox.randomBytes(16),payload,TransportKind.LAN);
        e=e.withSignature(identity.sign(e.unsignedBytes()));
        scanState.setText("در حال ارسال به "+recipients.size()+" دستگاه..."); send.setEnabled(false);
        final TransferEnvelope env=e;
        sendPool.execute(()->{
            int ok=0,fail=0;
            try(MultiRecipientDispatcher d=new MultiRecipientDispatcher(4)){
                Map<String,CompletableFuture<DeliveryAck>> jobs=d.sendAll(lanTransport,recipients,env);
                for(CompletableFuture<DeliveryAck> f:jobs.values())try{f.get(35,TimeUnit.SECONDS);ok++;}catch(Exception ex){fail++;}
            }
            int success=ok,failed=fail;runOnUiThread(()->{send.setEnabled(true);scanState.setText("تحویل‌شده: "+success+"  •  ناموفق: "+failed);});
        });
    }

    private boolean approveOutgoingPeerBlocking(String deviceId,String fingerprint,String verificationCode) {
        TrustedDevice known=trust.get(deviceId);
        if(known!=null && known.state==RelationshipState.TRUSTED && known.fingerprint.equals(fingerprint)) return true;
        final CountDownLatch latch=new CountDownLatch(1); final boolean[] ok={false};
        runOnUiThread(()->new AlertDialog.Builder(this)
            .setTitle("تایید دستگاه مقصد")
            .setMessage("کد تطبیق روی هر دو گوشی باید یکسان باشد:\n\n"+verificationCode+"\n\nشناسه مقصد: "+shortId(deviceId))
            .setPositiveButton("کد یکسان است",(d,w)->{ok[0]=true;latch.countDown();})
            .setNegativeButton("لغو",(d,w)->latch.countDown())
            .setOnCancelListener(d->latch.countDown()).show());
        try{latch.await(25,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();return false;} return ok[0];
    }

    private boolean approveUnknownBlocking(String deviceId,String fingerprint,String verificationCode,TransferEnvelope envelope) {
        final CountDownLatch latch=new CountDownLatch(1); final int[] decision={0};
        runOnUiThread(()->new AlertDialog.Builder(this)
            .setTitle("درخواست دریافت OmniShare")
            .setMessage("یک دستگاه Mahyar روی همین شبکه می‌خواهد کارت خود را ارسال کند.\nشناسه: "+shortId(deviceId)+"\nکد تطبیق: "+verificationCode+"\nکد باید روی هر دو گوشی یکسان باشد.")
            .setPositiveButton("همیشه اجازه بده",(d,w)->{long n=System.currentTimeMillis();trust.put(new TrustedDevice(deviceId,fingerprint,"Mahyar "+shortId(deviceId),RelationshipState.TRUSTED,true,n,n));decision[0]=2;latch.countDown();})
            .setNeutralButton("فقط این بار",(d,w)->{long n=System.currentTimeMillis();trust.put(new TrustedDevice(deviceId,fingerprint,"Mahyar "+shortId(deviceId),RelationshipState.KNOWN,false,n,n));decision[0]=1;latch.countDown();})
            .setNegativeButton("رد",(d,w)->{decision[0]=-1;latch.countDown();})
            .setOnCancelListener(d->{decision[0]=-1;latch.countDown();}).show());
        try{latch.await(25,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();return false;} return decision[0]>0;
    }

    private void showIncoming(TransferEnvelope e) {
        try {
            Profile p=Profile.fromJson(new String(e.payload,StandardCharsets.UTF_8));
            runOnUiThread(()->new AlertDialog.Builder(this).setTitle("کارت دریافت شد").setMessage(p.name+"\n"+p.phone+"\n"+p.email)
                .setPositiveButton("ذخیره مخاطب",(d,w)->openContact(p)).setNegativeButton("بستن",null).show());
        } catch(Exception ex) { runOnUiThread(()->Toast.makeText(this,"کارت دریافت شد اما فرمت آن نامعتبر بود",Toast.LENGTH_LONG).show()); }
    }
    private void openContact(Profile p){Intent i=new Intent(ContactsContract.Intents.Insert.ACTION);i.setType(ContactsContract.RawContacts.CONTENT_TYPE);i.putExtra(ContactsContract.Intents.Insert.NAME,p.name);i.putExtra(ContactsContract.Intents.Insert.PHONE,p.phone);i.putExtra(ContactsContract.Intents.Insert.EMAIL,p.email);startActivity(i);}
    private static String shortId(String id){return id==null?"????":id.substring(0,Math.min(8,id.length()));}

    @Override protected void onDestroy(){if(bleScanner!=null)bleScanner.close();if(lanTransport!=null)lanTransport.close();else if(lanDiscovery!=null)lanDiscovery.close();if(lanServer!=null)lanServer.close();if(sendPool!=null)sendPool.shutdownNow();super.onDestroy();}
}
