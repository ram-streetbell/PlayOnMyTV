<style>
    .download-page{min-height:100vh;padding:48px 20px 64px;background:radial-gradient(circle at top,#eef3f8 0,#f7f8fa 42%,#fff 100%)}
    .download-wrap{max-width:1080px;margin:0 auto}
    .download-brand{font-size:18px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:#17202a;margin-bottom:70px}
    .download-hero{text-align:center;margin-bottom:48px}
    .download-hero h1{font-size:clamp(40px,6vw,68px);font-weight:700;letter-spacing:-.04em;color:#111827;margin:0 0 16px}
    .download-hero p{max-width:680px;margin:0 auto;color:#667085;font-size:19px;line-height:1.65}
    .download-version{display:inline-flex;align-items:center;margin-top:24px;padding:7px 13px;border:1px solid #d9dee7;border-radius:999px;background:#fff;color:#475467;font-size:14px;font-weight:600}
    .download-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:24px}
    .download-card{background:#fff;border:1px solid #e4e7ec;border-radius:24px;padding:32px;box-shadow:0 16px 40px rgba(16,24,40,.06);display:flex;flex-direction:column;min-height:330px}
    .download-card h2{font-size:28px;color:#101828;margin:0 0 10px}
    .download-card .platform{color:#667085;margin-bottom:22px;font-size:15px}
    .download-features{list-style:none;padding:0;margin:0 0 28px;color:#475467;line-height:1.9;flex:1}
    .download-features li:before{content:'✓';margin-right:10px;font-weight:700;color:#344054}
    .download-button{display:inline-flex;justify-content:center;align-items:center;width:100%;padding:13px 18px;border-radius:12px;text-decoration:none;font-weight:700;background:#111827;color:#fff;transition:transform .15s ease,opacity .15s ease}
    .download-button:hover{color:#fff;opacity:.9;transform:translateY(-1px)}
    .download-note{text-align:center;color:#667085;font-size:14px;margin:30px 0 0}
    .release-link{text-align:center;margin-top:22px}
    .release-link a{color:#344054;font-weight:600;text-decoration:none}
    @media(max-width:760px){.download-page{padding-top:30px}.download-brand{margin-bottom:48px}.download-grid{grid-template-columns:1fr}.download-card{min-height:0}}
</style>

<main class="download-page">
    <div class="download-wrap">
        <div class="download-brand">PlayOnMyTV</div>

        <section class="download-hero">
            <h1>Download PlayOnMyTV</h1>
            <p>Install the PlayOnMyTV digital signage player on your TV and connect it to your PlayOnMyTV dashboard.</p>
            <div class="download-version">Latest release: v<?= htmlspecialchars($version, ENT_QUOTES, 'UTF-8') ?></div>
        </section>

        <section class="download-grid" aria-label="PlayOnMyTV applications">
            <article class="download-card">
                <h2>Android TV</h2>
                <div class="platform">APK installer for Android TV and compatible Android TV devices.</div>
                <ul class="download-features">
                    <li>Automatic device pairing</li>
                    <li>Image and video playback</li>
                    <li>Playlists and scheduled content</li>
                    <li>Offline media caching</li>
                    <li>Automatic synchronization</li>
                </ul>
                <a class="download-button" href="<?= htmlspecialchars($androidUrl, ENT_QUOTES, 'UTF-8') ?>">Download Android TV APK</a>
            </article>

            <article class="download-card">
                <h2>LG webOS</h2>
                <div class="platform">IPK installer for supported LG webOS televisions.</div>
                <ul class="download-features">
                    <li>6-digit device pairing</li>
                    <li>Image and video playback</li>
                    <li>Playlists and scheduled content</li>
                    <li>Offline media caching</li>
                    <li>Automatic synchronization</li>
                </ul>
                <a class="download-button" href="<?= htmlspecialchars($webosUrl, ENT_QUOTES, 'UTF-8') ?>">Download LG webOS IPK</a>
            </article>
        </section>

        <p class="download-note">After installation, pair your TV from the PlayOnMyTV dashboard using the device code shown on the TV.</p>
        <div class="release-link"><a href="<?= htmlspecialchars($releaseUrl, ENT_QUOTES, 'UTF-8') ?>">View release notes and previous versions</a></div>
    </div>
</main>
