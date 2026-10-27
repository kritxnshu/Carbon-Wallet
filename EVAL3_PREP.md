# Eval 3 prep: Implementation & Quality

Total 100 marks: Implementation quality 20 · Innovation 20 · Usability & accessibility 15 · Testing & robustness 25 ·
Presentation & Q&A 20. Each section below gives the claim to make, the proof to show, and what to say out loud.

---

## 1. Implementation quality (20)

**Claim:** every feature we show works end to end, on the live site and in three native apps, on one shared backend.

**Show (in this order, all live):**
1. Ghar app → My home: 229 kg, Green, 57% of 400 kg (4 people × 100 kg), "one thing to do".
2. Bill tab → Consumer ID card on top → take a bill photo → "check these numbers" → confirm.
3. Mohalla tab → home vs mohalla vs city vs state vs India → Heat map → switch city.
4. Dukaan app → scan the printed Carbon Card (CW1-GF7Z-8014-J97H) → colour + schemes → register an AC.
5. Ghar app → notification "Demo Appliances added an AC" → item shows under Card & items.
6. Admin app / website → shop approval, removal requests, help desk.

**Say:** "One Next.js server serves the website and all three apps, so a rule lives in one place. The Python engine
does the maths and bill reading. Data enters at only two points, the bill and the shop counter."

**Completeness checklist (all built):** bill OCR with confirm step · AC calibration from bills + weather · monthly
totals and colour · recommendations with ₹ and kg · mohalla/city/state/India averages · heat map · Carbon Card with
barcode + NFC · shop counter with 15-minute tickets · scheme finder · removal disputes · grievances · proof sealing ·
data export and erase · 22 languages · three signed Android apps.

---

## 2. Innovation (20)

Pick three and say them clearly:

1. **Zero logging.** Carbon apps die because people stop logging. We need one bill photo a month; shops register
   purchases. Nothing to type daily.
2. **The AC measured from the home's own bills.** Star labels under-count real AC use about 2.6× (Prayas field data).
   We fit `bill kWh = baseload × days + AC kW × AC hours` across the home's bills and the weather.
3. **Carbon at the point of purchase.** The shop scans the family's card and sees the home's colour and the subsidy
   that fits (PM Surya Ghar, PM E-DRIVE) at the moment a 10-year decision (3★ vs 5★ AC) is made.
4. **Privacy by design.** The card's barcode is a random code only our server can resolve; shops see colour, never
   bills; areas show only with 20+ homes; months are sealed on chain as salted hashes, and deleting the salt
   crypto-shreds the link for DPDP erasure.
5. **Built for India.** Hindi first, 22 languages, ₹ next to every kg, apps of 2.5–13 MB for a ₹8,000 phone.

---

## 3. Usability & accessibility (15)

**Say and show:**
- One number, one colour, one action per screen. The colour always comes with an icon and a word (Green · within your
  allowance), never colour alone, so colour-blind users can read it.
- Text size A / A+ / A++ and Hindi / English on every screen (top bar).
- Big tap targets (buttons 44–48 dp), plain words ("Bill", "Mohalla", "Card & items", "Schemes").
- The Card tab turns the screen to full brightness so scanners read it first time.
- Apps open instantly from saved data and show "Showing saved data" when offline, instead of a blank screen.
- Three roles, three apps: a shopkeeper never sees family screens, so each flow is short.

**If asked about screen readers:** charts and meters carry text labels (`aria-label` on the web, content descriptions
in the apps); a full TalkBack audit is on our list.

---

## 4. Testing & technical robustness (25), the biggest section

**Evidence to show (run them live or show screenshots):**
- Website: `npx vitest run` → **33 files, 177 tests, all passing** (auth and permissions, bills, cards, card code,
  privacy, merchants, schemes, chain jobs, Google sign-in, statements, heat wave, languages, demo rehearsal).
- Type check `npx tsc --noEmit` and `npm run lint`: clean.
- Engine: `uv run pytest` → **131 passing**; the 7 OCR photo tests need the Tesseract binary, which the Docker image
  installs (they fail only on a laptop without it).
- Android: release builds run on an emulator and a real phone.

**Edge cases we handle (say 4–5 of these):**
| Situation | What happens |
|---|---|
| Bill misread by OCR | Nothing counts until the family confirms; AI fallback only with consent |
| Bill uploaded twice / wrong bill | Deleting it removes its kWh, rebuilds the months, the AC calibration and the suggestions |
| Two shop tills register at once | The 10-items-per-scan limit is re-checked inside a database transaction |
| Scan ticket reused later | Tickets expire after 15 minutes; only their hash is stored |
| Someone scans a shared public link at a shop | Refused: only the card's private code works at the counter |
| Shop adds an item the family didn't buy | "Not ours" → removal request with photos → admin decides; counts against the shop |
| Lost card | Reissue: new code, every old printed card stops working at once |
| Engine asleep or slow | 25-second timeouts with "offline or waking up" messages; the app retries a failed call once |
| No internet on the phone | Screens open from saved data with a banner |
| Every input | Checked with Zod schemas on the server; rate limits on login and card lookups |
| Release-only crash (Dukaan camera) | Found with the R8 mapping file, fixed with keep rules and a fallback |

**Stability for the demo:** the live site's slowness came from hosting: the website ran in Ohio while the database is
in Singapore, so each of ~30 queries crossed the world (~25 s). Fix: the website now runs in Singapore next to the
database (Vercel `sin1`, pinned in `vercel.json`, live at https://carbon-wallet-india.vercel.app), and the engine is
pinged every 10 minutes so it never sleeps. Measured on 27 Oct 2026:

| Screen | Before (Ohio) | After (Singapore) |
|---|---|---|
| Home (overview) | 25–27 s | 0.6 s (2.2 s first call) |
| Mohalla | 23–25 s | 0.7 s |
| Bills | 8.5 s | 0.5 s |
| Heat map | — | 0.5 s | Say this as a finding: "we
measured it, found the cause and moved compute next to data".

---

## 5. Presentation & Q&A (20)

### 5-minute demo script
1. (30 s) Problem: ACs in India emit as much as all cars; families can't see their carbon; logging apps get dropped.
2. (60 s) Ghar app: colour, allowance maths, one thing to do, bill photo → confirm.
3. (60 s) Dukaan app: scan the printed card → colour + PM Surya Ghar → register a 5★ AC → family notified.
4. (45 s) Mohalla + heat map: privacy threshold of 20 homes.
5. (45 s) Proof page: a sealed month verifies; change one number and it fails.
6. (60 s) Testing and robustness slide: tests, edge cases, the latency fix.

### Likely questions and answers
- **How accurate are your numbers?** Electricity uses CEA's grid factor (0.675 kg/kWh, FY 2025-26). AC power comes from
  the home's own bills. Every number shows its source; AI never produces a number.
- **Is the data real?** The demo dataset is mostly synthetic (1,833 homes, 47 mohallas) so the area features work;
  bill reading and shop scanning work on real bills and real cards today.
- **Why 100 kg per person?** A simple, fixed yardstick families understand. It's a budget, not a legal limit.
- **Why does the Sharma home emit more than India's average?** India's average includes homes without AC or a car;
  Sharma has two ACs and a car in NCR. Being above a national average is normal for a city home.
- **What if a shop cheats?** Shops are approved by an admin, tickets last 15 minutes, families can dispute, disputes
  count against the shop and admins can suspend it.
- **Privacy?** Shops never see bills; areas need 20+ homes; files are AES-256 encrypted; nothing personal goes on
  chain; export and erase are built in (DPDP).
- **Why blockchain at all?** So a bank or employer can check a sealed month without trusting us. One batch a day on
  Polygon (proof-of-stake), so the footprint is negligible. No tokens.
- **Why not Hyperledger Fabric?** Fabric is permissioned: a verifier would still have to trust whoever runs the peers.
  It fits later, when discoms and banks each run a peer. We keep the anchoring layer swappable.
- **It was slow on the live site. Why?** Hosting regions: website in Ohio, database in Singapore. We moved the website
  next to the database and keep the engine awake.
- **How do you scale?** Pages read pre-computed monthly totals; heavy work runs in nightly jobs; the engine is
  stateless and can run more copies.
- **Air quality number?** Today it's the CAMS model estimate from Open-Meteo; next we switch to CPCB station data.
- **What would you build next?** Smart-meter data, WhatsApp bill bot, voice in Indian languages (Bhashini), Play Store.

---

## Before the eval (checklist)
- [ ] 10 minutes before: open the live site and each app once (warms everything up).
- [ ] Phone charged; Ghar, Dukaan and Admin installed (APKs in `~/Desktop/CarbonWallet-APKs/`); signed in.
- [ ] Printed Carbon Card in hand; Dukaan camera permission already allowed.
- [ ] Backup: run `npm run demo` on the laptop (local database) in case the venue's internet fails.
- [ ] Terminal ready with `npx vitest run` for the testing section.
