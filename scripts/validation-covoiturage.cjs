// Opt-in : une instance dev de démonstration, sur une base isolée neuve uniquement.
// Outillage facultatif : npm install --prefix target/browser-validation --no-save --package-lock=false playwright@1.56.1
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const api = process.env.VALIDATION_API_URL || "http://127.0.0.1:18081";
const front = process.env.VALIDATION_FRONTEND_URL || "http://127.0.0.1:4201";
const database = process.env.VALIDATION_DATABASE || "";
if (
  !/^(transport_demo_|issue1_validation_)[a-z0-9_]+$/.test(database) ||
  !["localhost", "127.0.0.1"].includes(new URL(api).hostname) ||
  !["localhost", "127.0.0.1"].includes(new URL(front).hostname)
) {
  throw new Error(
    "Validation autorisée uniquement avec une base isolée nommée et des URL locales.",
  );
}
const password = process.env.DEMO_PASSWORD;
if (!password || password.length < 12)
  throw new Error("DEMO_PASSWORD est requis, sans affichage de sa valeur.");

const report = { database, controles: [], responsive: [] };
function ok(controle) {
  report.controles.push(controle);
}
async function call(method, route, token, body, expected = 200) {
  const response = await fetch(api + route, {
    method,
    signal: AbortSignal.timeout(20000),
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body ? { "Content-Type": "application/json" } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  assert.ok(
    (Array.isArray(expected) ? expected : [expected]).includes(response.status),
    `${method} ${route} : HTTP ${response.status}, attendu ${expected}`,
  );
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
async function login(matricule) {
  return call("POST", "/api/voyageurs/login", null, { matricule, password });
}
function depart(jours, heure) {
  const date = new Date();
  date.setDate(date.getDate() + jours);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}T${heure}:00`;
}

(async () => {
  for (let i = 0; i < 90; i++) {
    try {
      if (
        (
          await fetch(api + "/v3/api-docs", {
            signal: AbortSignal.timeout(1500),
          })
        ).ok
      )
        break;
    } catch {
      /* démarrage en cours */
    }
    if (i === 89) throw new Error("Le backend de validation ne démarre pas.");
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  const admin = await call("POST", "/api/auth/login", null, {
    email: "admin@transp.com",
    password,
  });
  const localAdmin = await call("POST", "/api/auth/login", null, {
    email: "admin.lyon@transp.com",
    password,
  });
  const chauffeur = await login("V1001");
  const clients = await Promise.all(["V1002", "L10001", "F10001"].map(login));
  const agences = await call("GET", "/api/agences");
  assert.equal(agences.length, 1);
  assert.match(agences[0].nom, /DÉMONSTRATION/);
  assert.equal((await call("GET", "/api/voyageurs", admin.token)).length, 4);
  assert.equal(chauffeur.peutProposerTrajet, true);
  const initial = await call(
    "GET",
    "/api/trajets/public?depart=MONS&arrivee=BRUXELLES",
  );
  assert.equal(initial.length, 1);
  assert.equal(initial[0].placesRestantes, 4);
  const trajet = initial[0];
  ok(
    "Connexions JWT administrateurs, chauffeur et trois catégories ; base de démonstration identifiée",
  );
  const req = {
    vehiculeId: trajet.vehiculeId,
    trajetDate: depart(3, "14:00"),
    trajetType: "PRIVE",
    villeDepart: "NAMUR",
    villeArrivee: "BRUXELLES",
    publicationAutomatiqueAutorisee: false,
  };
  await call("POST", "/api/trajets", admin.token, req, 400);
  await call(
    "POST",
    "/api/trajets",
    chauffeur.token,
    { ...req, chauffeurId: clients[0].id },
    400,
  );
  await call("POST", "/api/trajets", clients[0].token, req, 400);
  await call(
    "POST",
    "/api/trajets",
    chauffeur.token,
    { ...req, villeArrivee: "NAMUR" },
    400,
  );
  const prive = await call("POST", "/api/trajets", chauffeur.token, req, 201);
  assert.equal(prive.chauffeurId, chauffeur.id);
  assert.equal(prive.placesRestantes, 4);
  await call("GET", `/api/trajets/${prive.id}`, clients[0].token, null, 403);
  const invites = await call(
    "POST",
    "/api/reservations/private-invites",
    chauffeur.token,
    { trajetId: prive.id, inviteeMatricules: ["V1002"] },
    201,
  );
  await call("GET", `/api/trajets/${prive.id}`, clients[0].token);
  await call(
    "PATCH",
    `/api/trajets/${prive.id}/convert-public`,
    admin.token,
    {},
    400,
  );
  assert.equal(
    (
      await call(
        "GET",
        `/api/reservations/trajet/${prive.id}`,
        clients[1].token,
      )
    ).length,
    0,
  );
  assert.equal(
    (
      await call(
        "POST",
        `/api/paiements/reservation/${invites[0].id}/pay`,
        clients[0].token,
        {},
      )
    ).montant,
    25,
  );
  await call(
    "POST",
    `/api/paiements/reservation/${invites[0].id}/pay`,
    clients[0].token,
    {},
    400,
  );
  ok(
    "Chauffeur automatique, refus des villes identiques et du chauffeur manquant/usurpé, invitation et confidentialité privées, publication sans consentement refusée",
  );
  for (const matricule of ["V1003", "V1004"]) {
    await call(
      "POST",
      "/api/voyageurs",
      admin.token,
      {
        matricule,
        nom: "Test",
        prenom: "Client",
        typeVoyageur: "VIP",
        password,
        agenceId: agences[0].id,
      },
      201,
    );
    clients.push(await login(matricule));
  }
  await call(
    "POST",
    "/api/reservations",
    chauffeur.token,
    { trajetId: trajet.id, voyageurId: chauffeur.id },
    400,
  );
  const demandes = [];
  for (const client of clients)
    demandes.push(
      await call(
        "POST",
        "/api/reservations",
        client.token,
        { trajetId: trajet.id, voyageurId: client.id },
        201,
      ),
    );
  assert.equal(
    (await call("GET", `/api/trajets/${trajet.id}`, chauffeur.token))
      .placesRestantes,
    4,
  );
  for (let i = 0; i < 3; i++)
    assert.equal(
      (
        await call(
          "POST",
          `/api/paiements/reservation/${demandes[i].id}/pay`,
          clients[i].token,
          {},
        )
      ).montant,
      25,
    );
  const races = await Promise.all(
    [3, 4].map(async (i) => {
      const r = await fetch(
        `${api}/api/paiements/reservation/${demandes[i].id}/pay`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${clients[i].token}` },
        },
      );
      return { i, status: r.status };
    }),
  );
  assert.equal(races.filter((r) => r.status === 200).length, 1);
  assert.equal(races.filter((r) => r.status === 400).length, 1);
  let reservations = await call(
    "GET",
    `/api/reservations/trajet/${trajet.id}`,
    admin.token,
  );
  assert.equal(reservations.filter((r) => r.statut === "CONFIRMEE").length, 4);
  assert.equal(
    reservations
      .filter((r) => r.paiement.statut === "PAYE")
      .reduce((s, r) => s + r.paiement.montant, 0),
    100,
  );
  assert.equal(
    (await call("GET", `/api/trajets/${trajet.id}`, chauffeur.token))
      .placesRestantes,
    0,
  );
  const perdant = races.find((r) => r.status === 400).i;
  await call(
    "PATCH",
    `/api/reservations/${demandes[0].id}/cancel`,
    chauffeur.token,
    {},
    204,
  );
  assert.equal(
    (await call("GET", `/api/trajets/${trajet.id}`, chauffeur.token))
      .placesRestantes,
    1,
  );
  await call(
    "POST",
    `/api/paiements/reservation/${demandes[perdant].id}/pay`,
    clients[perdant].token,
    {},
  );
  reservations = await call(
    "GET",
    `/api/reservations/trajet/${trajet.id}`,
    admin.token,
  );
  assert.equal(
    reservations.find((r) => r.id === demandes[0].id).paiement.statut,
    "REMBOURSE",
  );
  ok(
    "Cinq demandes sans retenue de place ; quatre paiements de 25 €, 100 € encaissés ; concurrence dernière place, annulation chauffeur, remboursement et réutilisation",
  );

  const publicCree = await call(
    "POST",
    "/api/trajets",
    chauffeur.token,
    {
      ...req,
      trajetType: "PUBLIC",
      trajetDate: depart(4, "09:00"),
      villeDepart: "MONS",
    },
    201,
  );
  const autreVehicule = await call(
    "POST",
    "/api/vehicules",
    admin.token,
    { nom: "Voiture test conflit chauffeur", agenceId: agences[0].id },
    201,
  );
  await call(
    "POST",
    "/api/trajets",
    chauffeur.token,
    { ...req, vehiculeId: autreVehicule.id, trajetDate: depart(4, "09:00") },
    400,
  );
  await call("PUT", `/api/vehicules/${autreVehicule.id}`, admin.token, {
    nom: autreVehicule.nom,
    agenceId: agences[0].id,
    disponible: false,
  });
  await call(
    "POST",
    "/api/trajets",
    chauffeur.token,
    { ...req, vehiculeId: autreVehicule.id, trajetDate: depart(5, "09:00") },
    400,
  );
  const etrangere = await call(
    "POST",
    "/api/agences",
    admin.token,
    {
      nom: "Agence test hors périmètre",
      adresse: "Liège, Belgique",
      heureOuverture: "06:00",
      heureFermeture: "23:00",
      dureeTrajetMinutes: 90,
      dureeEntreTrajetMinutes: 15,
      anneeCivile: new Date().getFullYear(),
    },
    201,
  );
  const voitureEtrangere = await call(
    "POST",
    "/api/vehicules",
    admin.token,
    { nom: "Voiture hors périmètre", agenceId: etrangere.id },
    201,
  );
  const chauffeurEtranger = await call(
    "POST",
    "/api/voyageurs",
    admin.token,
    {
      matricule: "V1005",
      nom: "Test",
      prenom: "Chauffeur externe",
      typeVoyageur: "VIP",
      password,
      agenceId: etrangere.id,
      peutProposerTrajet: true,
    },
    201,
  );
  const reqEtranger = {
    ...req,
    vehiculeId: voitureEtrangere.id,
    chauffeurId: chauffeurEtranger.id,
    trajetDate: depart(4, "16:00"),
  };
  await call("POST", "/api/trajets", localAdmin.token, reqEtranger, [400, 403]);
  const externe = await call(
    "POST",
    "/api/trajets",
    admin.token,
    reqEtranger,
    201,
  );
  assert.ok(
    !(await call("GET", "/api/trajets", localAdmin.token)).some(
      (t) => t.id === externe.id,
    ),
  );
  ok(
    "Conflit chauffeur sur deux véhicules refusé ; création administrateur avec chauffeur et isolation de l’agence locale",
  );

  const {
    chromium,
  } = require(process.env.PLAYWRIGHT_MODULE || "../target/browser-validation/node_modules/playwright");
  const browser = await chromium.launch({
    executablePath:
      process.env.EDGE_PATH ||
      "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
    headless: true,
  });
  const page = await browser.newPage();
  const jsErrors = [];
  page.on("pageerror", (e) => jsErrors.push(e.message));
  const output = path.resolve(__dirname, "../target/browser-validation");
  fs.mkdirSync(output, { recursive: true });
  try {
    for (const width of [375, 768, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      await page.goto(front, { waitUntil: "networkidle" });
      await page
        .getByRole("heading", { name: "Voyagez ensemble, simplement." })
        .waitFor();
      assert.equal(
        await page.evaluate(() => document.documentElement.scrollWidth),
        width,
      );
      await page
        .getByRole("button", { name: "Réserver", exact: true })
        .first()
        .waitFor();
      await page.screenshot({
        path: path.join(output, `integration-accueil-${width}.png`),
        fullPage: true,
      });
      report.responsive.push({
        page: "accueil avec trajets réels",
        width,
        debordement: false,
      });
    }
    await page.getByRole("button", { name: "Choix de langue" }).click();
    await page.getByRole("button", { name: "English", exact: true }).click();
    await page
      .getByRole("heading", { name: "Travel together, simply." })
      .waitFor();
    ok("Accueil et cartes changent de langue via le mécanisme FR/EN existant");
    await page.evaluate(() =>
      localStorage.setItem("gestiontransport_language", "fr"),
    );
    await page.goto(front + "/voyageur", { waitUntil: "networkidle" });
    await page.locator("[formcontrolname=matricule]").fill("V1002");
    await page.locator("[formcontrolname=password]").fill(password);
    await page.locator("button[type=submit]").click();
    await page.waitForURL("**/voyageur/profile");
    await page.getByText("Voyages à venir", { exact: true }).waitFor();
    await page.goto(front + "/voyageur/trajets", { waitUntil: "networkidle" });
    const creationDepuisInterface = page.waitForResponse(
      (r) =>
        r.url().endsWith("/api/reservations") &&
        r.request().method() === "POST",
    );
    await page
      .locator(`[data-trajet-id="${publicCree.id}"]`)
      .getByRole("button", { name: "Réserver", exact: true })
      .click();
    const reponseCreation = await creationDepuisInterface;
    assert.equal(reponseCreation.status(), 201);
    const reservationInterface = await reponseCreation.json();
    await page.goto(front + "/voyageur/reservations", {
      waitUntil: "networkidle",
    });
    const carteReservation = page
      .locator("mat-card")
      .filter({
        has: page
          .locator("mat-card-title")
          .filter({
            hasText: new RegExp("#" + reservationInterface.id + "\\b"),
          }),
      });
    const paiementDepuisInterface = page.waitForResponse(
      (r) =>
        r
          .url()
          .endsWith(
            `/api/paiements/reservation/${reservationInterface.id}/pay`,
          ) && r.request().method() === "POST",
    );
    await carteReservation.getByRole("button", { name: /Payer/ }).click();
    const reponsePaiement = await paiementDepuisInterface;
    assert.equal(reponsePaiement.status(), 200);
    assert.equal((await reponsePaiement.json()).montant, 25);
    await carteReservation
      .getByText(/Confirmee|Confirmée|Confirmed/, { exact: true })
      .waitFor();
    assert.equal(
      (await call("GET", `/api/trajets/${publicCree.id}`, chauffeur.token))
        .placesRestantes,
      3,
    );
    ok(
      "Réservation et paiement de 25 € depuis les boutons Angular, confirmation et trois places restantes vérifiées en base via REST",
    );
    await page
      .getByText(/Confirmee|Confirmée|Confirmed/, { exact: true })
      .first()
      .waitFor();
    for (const width of [375, 768, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      assert.equal(
        await page.evaluate(() => document.documentElement.scrollWidth),
        width,
      );
      report.responsive.push({
        page: "réservations authentifiées",
        width,
        debordement: false,
      });
    }
    ok(
      "Connexion navigateur avec JWT, profil, réservations et historique de paiement",
    );
    await page.evaluate(() =>
      localStorage.removeItem("gestiontransport_voyageur_session"),
    );
    await page.goto(front + "/admin/login", { waitUntil: "networkidle" });
    await page.locator("[formcontrolname=email]").fill("admin@transp.com");
    await page.locator("[formcontrolname=password]").fill(password);
    await page.locator("button[type=submit]").click();
    await page.waitForURL("**/admin");
    await page.getByText("150 EUR", { exact: true }).waitFor();
    for (const width of [375, 768, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      await page
        .getByRole("heading", { name: "Administration globale", exact: true })
        .waitFor();
      assert.equal(
        await page.evaluate(() => document.documentElement.scrollWidth),
        width,
      );
      report.responsive.push({
        page: "administration authentifiée",
        width,
        debordement: false,
      });
    }
    ok("Administration et statistiques réelles chargées dans le navigateur");
    await page.evaluate(() => localStorage.removeItem("gestiontransport_admin_session"));
    await page.goto(front + "/admin/login", { waitUntil: "networkidle" });
    await page.locator("[formcontrolname=email]").fill("admin.lyon@transp.com");
    await page.locator("[formcontrolname=password]").fill(password);
    await page.locator("button[type=submit]").click();
    await page.waitForURL("**/admin");
    await page.getByRole("heading", {name:"Administration de mon agence",exact:true}).waitFor();
    for (const width of [375,768,1440]) {
      await page.setViewportSize({width,height:900});
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth),width);
      report.responsive.push({page:"administration agence authentifiée",width,debordement:false});
    }
    await page.goto(front + "/admin/vehicules", {waitUntil:"networkidle"});
    assert.ok(!(await page.locator("main").innerText()).includes(voitureEtrangere.nom));
    ok("Connexion navigateur AGENCE, dashboard local et véhicule hors périmètre absent");
    await page.evaluate(() =>
      localStorage.removeItem("gestiontransport_admin_session"),
    );
    await page.goto(front + "/voyageur", { waitUntil: "networkidle" });
    await page.locator("[formcontrolname=matricule]").fill("V1001");
    await page.locator("[formcontrolname=password]").fill(password);
    await page.locator("button[type=submit]").click();
    await page.waitForURL("**/voyageur/profile");
    page.once("dialog", (dialog) => dialog.accept());
    const annulation = page.waitForResponse(
      (r) =>
        r
          .url()
          .endsWith(
            `/api/trajets/${prive.id}/cancel?requesterId=${chauffeur.id}`,
          ) && r.request().method() === "PATCH",
    );
    await page
      .getByRole("button", {
        name: `Annuler le trajet #${prive.id}`,
        exact: true,
      })
      .click();
    assert.equal((await annulation).status(), 204);
    const annulees = await call(
      "GET",
      `/api/reservations/trajet/${prive.id}`,
      admin.token,
    );
    assert.equal(annulees[0].statut, "ANNULEE");
    assert.equal(annulees[0].paiement.statut, "REMBOURSE");
    ok(
      "Annulation du trajet par le chauffeur dans le navigateur et remboursement conservé dans l’historique",
    );
    assert.deepEqual(jsErrors, []);
    ok("Aucune erreur JavaScript");
    report.trajetPublicCree = publicCree.id;
    fs.writeFileSync(
      path.join(output, "integration-rapport.json"),
      JSON.stringify(report, null, 2),
    );
    console.log(JSON.stringify(report));
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
