package com.mahweb.mahyarnfc.omnishare.transfer;
import com.mahweb.mahyarnfc.omnishare.TransferEnvelope; import java.util.*; import java.util.Base64;
public final class TransferLedger {
 public enum AcceptDecision { ACCEPT_NEW, DUPLICATE_ALREADY_DELIVERED, REJECT_EXPIRED, REJECT_REPLAY }
 private final LinkedHashMap<String,Long> delivered=new LinkedHashMap<>(); private final LinkedHashMap<String,String> nonces=new LinkedHashMap<>();
 public synchronized AcceptDecision evaluate(TransferEnvelope e,long now){if(!e.isValidAt(now))return AcceptDecision.REJECT_EXPIRED;if(delivered.containsKey(e.transferId))return AcceptDecision.DUPLICATE_ALREADY_DELIVERED;String nk=e.senderDeviceId+":"+Base64.getUrlEncoder().withoutPadding().encodeToString(e.nonce);String prior=nonces.get(nk);if(prior!=null&&!prior.equals(e.transferId))return AcceptDecision.REJECT_REPLAY;nonces.put(nk,e.transferId);trim(nonces,1000);return AcceptDecision.ACCEPT_NEW;}
 public synchronized void markDelivered(String id,long now){delivered.put(id,now);trim(delivered,1000);} private static <K,V>void trim(LinkedHashMap<K,V>m,int n){while(m.size()>n)m.remove(m.keySet().iterator().next());}
}
