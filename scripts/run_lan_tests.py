from pathlib import Path
import subprocess, tempfile, sys
ROOT=Path(__file__).resolve().parents[1]
src=ROOT/'app/src/main/java'
files=[
 'com/mahweb/mahyarnfc/omnishare/TransferState.java','com/mahweb/mahyarnfc/omnishare/TransportKind.java','com/mahweb/mahyarnfc/omnishare/PayloadType.java','com/mahweb/mahyarnfc/omnishare/TransferEnvelope.java','com/mahweb/mahyarnfc/omnishare/TransferEnvelopeCodec.java',
 'com/mahweb/mahyarnfc/omnishare/crypto/CryptoEnvelope.java','com/mahweb/mahyarnfc/omnishare/crypto/CryptoBox.java',
 'com/mahweb/mahyarnfc/omnishare/identity/DeviceIdentity.java','com/mahweb/mahyarnfc/omnishare/identity/IdentityStore.java',
 'com/mahweb/mahyarnfc/omnishare/trust/RelationshipState.java','com/mahweb/mahyarnfc/omnishare/trust/TrustedDevice.java','com/mahweb/mahyarnfc/omnishare/trust/TrustPolicy.java','com/mahweb/mahyarnfc/omnishare/trust/TrustLookup.java',
 'com/mahweb/mahyarnfc/omnishare/transfer/TransferLedger.java',
 'com/mahweb/mahyarnfc/omnishare/transport/Recipient.java','com/mahweb/mahyarnfc/omnishare/transport/TransportCandidate.java','com/mahweb/mahyarnfc/omnishare/transport/DeliveryAck.java','com/mahweb/mahyarnfc/omnishare/transport/DeliveryAckValidator.java','com/mahweb/mahyarnfc/omnishare/transport/OmniTransport.java',
 'com/mahweb/mahyarnfc/omnishare/lan/LanProtocol.java','com/mahweb/mahyarnfc/omnishare/lan/LanPeerVerifier.java','com/mahweb/mahyarnfc/omnishare/lan/Frame.java','com/mahweb/mahyarnfc/omnishare/lan/FrameCodec.java','com/mahweb/mahyarnfc/omnishare/lan/LanPeer.java','com/mahweb/mahyarnfc/omnishare/lan/DeliveryAckCodec.java','com/mahweb/mahyarnfc/omnishare/lan/SecurePacketCodec.java','com/mahweb/mahyarnfc/omnishare/lan/LanHandshake.java','com/mahweb/mahyarnfc/omnishare/lan/LanReceivePolicy.java','com/mahweb/mahyarnfc/omnishare/lan/LanServer.java','com/mahweb/mahyarnfc/omnishare/lan/LanClient.java','com/mahweb/mahyarnfc/omnishare/lan/MultiRecipientDispatcher.java',
]
with tempfile.TemporaryDirectory() as td:
    cmd=['javac','--release','17','-d',td]+[str(src/f) for f in files]+[str(ROOT/'scripts/harness/LanHarness.java')]
    cp=subprocess.run(cmd,text=True,capture_output=True)
    if cp.returncode:
        print(cp.stdout);print(cp.stderr);sys.exit(cp.returncode)
    r=subprocess.run(['java','-cp',td,'LanHarness'],text=True,capture_output=True)
    print(r.stdout,end='');print(r.stderr,end='')
    sys.exit(r.returncode)
