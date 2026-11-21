fn main() {
    #[cfg(windows)]
    {
        let mut res = winresource::WindowsResource::new();
        res.set_icon("icons/icon.ico");

        let version = env!("CARGO_PKG_VERSION");
        res.set("CompanyName", "Zyro");
        res.set("ProductName", "Zyro Client");
        res.set("FileDescription", "Zyro Client");
        res.set("LegalCopyright", "© 2026 Zyro");
        res.set("OriginalFilename", "zyro_app.exe");
        res.set("InternalName", "zyro_app");
        res.set("FileVersion", version);
        res.set("ProductVersion", version);

        res.compile()
            .expect("failed to embed Windows icon resource");
    }
}
