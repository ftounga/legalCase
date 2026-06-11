import pathlib
base = pathlib.Path("/home/francky/dev/legalCase/docs/marketing/encart-ace-plaquette")
logo_b64 = (base / ".logo.b64").read_text().strip()

html = """<!DOCTYPE html>
<html lang="fr"><head><meta charset="utf-8">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Merriweather:wght@700&display=swap" rel="stylesheet">
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  :root{
    --navy:#1A3A5C; --gold:#C9973A; --bg:#F5F6FA; --surface:#FFFFFF;
    --text:#1C2B3A; --muted:#6B7A8D; --divider:#E0E4EA;
  }
  html,body{ background:#ffffff; }
  .card{
    width:1080px; min-height:720px; background:var(--surface);
    font-family:'Inter',sans-serif; color:var(--text);
    position:relative; overflow:hidden;
    border:1px solid var(--divider);
  }
  .topbar{ height:10px; background:linear-gradient(90deg,var(--navy) 0%,var(--navy) 70%,var(--gold) 100%); }
  .pad{ padding:54px 64px 40px; }
  .head{ display:flex; align-items:center; gap:26px; }
  .head img{ width:210px; height:auto; }
  .head .eyebrow{ font-size:15px; letter-spacing:.16em; text-transform:uppercase; color:var(--gold); font-weight:600; }
  .head .partner{ font-size:20px; color:var(--muted); font-weight:500; margin-top:4px; }
  h1{ font-family:'Merriweather',serif; color:var(--navy); font-size:34px; line-height:1.25; margin-top:38px; }
  .lead{ font-size:19px; line-height:1.7; color:var(--text); margin-top:20px; max-width:900px; }
  .lead b{ color:var(--navy); font-weight:600; }
  .offer{
    margin-top:34px; background:var(--bg); border:1px solid var(--divider);
    border-left:6px solid var(--gold); border-radius:10px; padding:28px 34px;
    display:flex; align-items:center; justify-content:space-between; gap:30px;
  }
  .offer .otxt{ font-size:18px; line-height:1.6; color:var(--text); }
  .offer .otxt .otitle{ font-weight:700; color:var(--navy); font-size:19px; display:block; margin-bottom:6px; }
  .offer .otxt small{ color:var(--muted); }
  .code{
    flex:0 0 auto; text-align:center; background:var(--navy); color:#fff;
    border-radius:10px; padding:18px 26px; min-width:210px;
  }
  .code .label{ font-size:12px; letter-spacing:.14em; text-transform:uppercase; color:#aebfd2; }
  .code .val{ font-family:'Inter',sans-serif; font-weight:700; font-size:30px; letter-spacing:.04em; margin-top:6px; }
  .code .val span{ color:var(--gold); }
  .foot{
    margin:38px 64px 0; border-top:1px solid var(--divider); padding-top:22px;
    display:flex; align-items:center; justify-content:space-between;
  }
  .foot .who{ font-size:15px; color:var(--muted); line-height:1.55; }
  .foot .who b{ color:var(--text); font-weight:600; }
  .foot .url{ font-family:'Inter',sans-serif; font-weight:700; font-size:20px; color:var(--navy); }
  .foot .url span{ color:var(--gold); }
</style></head>
<body>
  <div class="card">
    <div class="topbar"></div>
    <div class="pad">
      <div class="head">
        <img src="data:image/png;base64,__LOGO__" alt="LegalCase">
        <div>
          <div class="eyebrow">Offre partenaire</div>
          <div class="partner">Réservée aux adhérents de l'ACE</div>
        </div>
      </div>

      <h1>L'analyse de dossier, de la pièce aux conclusions</h1>

      <p class="lead">
        LegalCase aide les cabinets à <b>gagner du temps sur leurs dossiers</b> : vous déposez
        les pièces, l'outil les analyse, fait ressortir les points clés, chiffre les enjeux et
        propose des <b>conclusions argumentées</b>, sources de jurisprudence à l'appui.
        Un gain de productivité concret, au service du jugement de l'avocat.
      </p>

      <div class="offer">
        <div class="otxt">
          <span class="otitle">30 jours d'essai supplémentaires</span>
          Sur l'ensemble des formules, sans engagement.<br>
          <small>Valable jusqu'au 31 décembre 2026.</small>
        </div>
        <div class="code">
          <div class="label">Code adhérent</div>
          <div class="val">ACE<span>2026</span></div>
        </div>
      </div>
    </div>

    <div class="foot">
      <div class="who">
        <b>Franck Tounga</b> — Fondateur, LegalCase<br>
        tounga.franck@ng-itconsulting.com
      </div>
      <div class="url">legalcase<span>.fr</span></div>
    </div>
  </div>
</body></html>"""

html = html.replace("__LOGO__", logo_b64)
(base / "encart-ace.html").write_text(html, encoding="utf-8")
print("HTML written:", (base / "encart-ace.html").stat().st_size, "bytes")
