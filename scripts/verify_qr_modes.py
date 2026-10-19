from pathlib import Path

root = Path(__file__).resolve().parents[1]
main = (root / "app/src/main/java/com/mahweb/mahyarnfc/MainActivity.java").read_text(encoding="utf-8")
universal = (root / "app/src/main/java/com/mahweb/mahyarnfc/UniversalCardPayload.java").read_text(encoding="utf-8")
assert "UniversalCardPayload.bridgeUrl(p, UniversalCardPayload.DEFAULT_BRIDGE_BASE_URL)" in main
assert "QrUtil.create(onlineUrl, size)" in main
assert "QrUtil.create(p.toVCard(), size)" in main
assert "https://mahyarmolavi.ir/nfc/" in universal
assert 'offline.setOnClickListener' in main and 'online.setOnClickListener' in main
print("QR_MODES_CONTRACT=PASS")
