# Linux Signing Guide

For Linux distributions (specifically `.deb` packages), signing is typically done using GPG.

## 1. Generate GPG Key
If you don't have one, generate it:
```bash
gpg --full-generate-key
```

## 2. Export Public Key
```bash
gpg --armor --export your-email@example.com > public.key
```

## 3. Sign the .deb package
After running `./gradlew packageDeb`, you can sign the resulting package:
```bash
dpkg-sig --sign builder composeApp/build/compose/binaries/main/deb/*.deb
```

Alternatively, for AppImage, you can use `appimagetool` with the `--sign` flag.
