// Odisea Galáctica — puzzle por niveles.
// El jugador resuelve cada nivel; la API resuelve lo mismo con el algoritmo del TPO y eso define las estrellas.
// Ningún algoritmo del TPO corre en el navegador: acá solo hay interacción, animación y sonido.
"use strict";

const $ = (id) => document.getElementById(id);
const SVG_NS = "http://www.w3.org/2000/svg";
const sinAnimacion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

/* =============================== Utilidades =============================== */

async function api(ruta, opciones = {}) {
    let respuesta;
    try {
        respuesta = await fetch(ruta, opciones);
    } catch (e) {
        throw new Error("No se pudo conectar con el servidor. ¿Está corriendo la aplicación?");
    }
    const texto = await respuesta.text();
    let datos = null;
    try { datos = texto ? JSON.parse(texto) : null; } catch (e) { /* sin JSON */ }
    if (!respuesta.ok) throw new Error((datos && datos.mensaje) || `Error ${respuesta.status}`);
    return datos;
}

const enviarJson = (ruta, cuerpo) => api(ruta, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(cuerpo)
});

function crear(etiqueta, clase, texto) {
    const el = document.createElement(etiqueta);
    if (clase) el.className = clase;
    if (texto !== undefined) el.textContent = texto;
    return el;
}

function svg(etiqueta, atributos = {}, texto) {
    const el = document.createElementNS(SVG_NS, etiqueta);
    for (const [k, v] of Object.entries(atributos)) el.setAttribute(k, v);
    if (texto !== undefined) el.textContent = texto;
    return el;
}

function avisar(mensaje) {
    const aviso = crear("div", "aviso", mensaje);
    $("avisos").appendChild(aviso);
    setTimeout(() => aviso.remove(), 6000);
}

const esperar = (ms) => new Promise((r) => setTimeout(r, sinAnimacion ? 0 : ms));
const numero = (n) => Number(n).toLocaleString("es-AR", { maximumFractionDigits: 2 });
const claveRuta = (a, b) => (a < b ? `${a}|${b}` : `${b}|${a}`);

function leer(clave, porDefecto) {
    try { const v = localStorage.getItem(clave); return v === null ? porDefecto : JSON.parse(v); } catch (e) { return porDefecto; }
}
function guardar(clave, valor) {
    try { localStorage.setItem(clave, JSON.stringify(valor)); } catch (e) { /* sin almacenamiento */ }
}

/** 3 estrellas si iguala al óptimo, 2 si queda a menos del 10 %, 1 si al menos completó el nivel. */
function calificar(eficiencia) {
    const e = Math.max(0, Math.min(1, eficiencia));
    return e >= 0.9999 ? 3 : e >= 0.9 ? 2 : 1;
}

/* =============================== Sonido (Web Audio, sin archivos) =============================== */

const sonido = (() => {
    let ctx = null, maestro, musica, efectos, eco;
    let activo = leer("odisea.sonido", true);
    let reloj = null, proximo = 0, paso = 0;

    const frecuencia = (midi) => 440 * Math.pow(2, (midi - 69) / 12);
    // La menor – Fa – Do – Sol: progresión ambiental en bucle
    const ACORDES = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]];
    const TIEMPO = 60 / 84 / 2; // corcheas a 84 bpm

    function iniciar() {
        if (ctx) return true;
        const Contexto = window.AudioContext || window.webkitAudioContext;
        if (!Contexto) return false;
        ctx = new Contexto();
        maestro = ctx.createGain();
        maestro.gain.value = activo ? 1 : 0;
        maestro.connect(ctx.destination);
        musica = ctx.createGain();
        musica.gain.value = 0.13;
        musica.connect(maestro);
        efectos = ctx.createGain();
        efectos.gain.value = 0.32;
        efectos.connect(maestro);
        // Eco para el arpegio
        eco = ctx.createDelay();
        eco.delayTime.value = TIEMPO * 3;
        const realimentacion = ctx.createGain();
        realimentacion.gain.value = 0.35;
        eco.connect(realimentacion);
        realimentacion.connect(eco);
        eco.connect(musica);
        return true;
    }

    function nota(midi, inicio, duracion, { tipo = "sine", volumen = 0.3, destino = musica, ataque = 0.02, filtro = null } = {}) {
        const osc = ctx.createOscillator();
        const env = ctx.createGain();
        osc.type = tipo;
        osc.frequency.value = frecuencia(midi);
        env.gain.setValueAtTime(0, inicio);
        env.gain.linearRampToValueAtTime(volumen, inicio + ataque);
        env.gain.exponentialRampToValueAtTime(0.0001, inicio + duracion);
        let salida = env;
        if (filtro) {
            const f = ctx.createBiquadFilter();
            f.type = "lowpass";
            f.frequency.value = filtro;
            env.connect(f);
            salida = f;
        }
        osc.connect(env);
        salida.connect(destino);
        osc.start(inicio);
        osc.stop(inicio + duracion + 0.05);
        return osc;
    }

    function programar() {
        while (proximo < ctx.currentTime + 0.25) {
            const compas = Math.floor(paso / 8) % ACORDES.length;
            const acorde = ACORDES[compas];
            if (paso % 8 === 0) {
                // Colchón: dos osciladores desafinados por nota, filtrados
                for (const m of acorde) {
                    for (const desafinado of [-6, 6]) {
                        const o = nota(m, proximo, TIEMPO * 8.5, { tipo: "sawtooth", volumen: 0.05, ataque: 0.9, filtro: 900 });
                        o.detune.value = desafinado;
                    }
                }
                nota(acorde[0] - 24, proximo, TIEMPO * 7.5, { tipo: "sine", volumen: 0.32, ataque: 0.05 });
            }
            // Arpegio que sube y baja sobre el acorde, una octava arriba
            const patron = [0, 1, 2, 1, 0, 2, 1, 2];
            const arpegio = nota(acorde[patron[paso % 8]] + 12, proximo, TIEMPO * 1.6, { tipo: "triangle", volumen: 0.09, destino: musica });
            if (paso % 2 === 0) {
                const env = ctx.createGain();
                env.gain.value = 0.5;
                arpegio.connect(env);
                env.connect(eco);
            }
            proximo += TIEMPO;
            paso++;
        }
    }

    function arrancarMusica() {
        if (!ctx || reloj) return;
        proximo = ctx.currentTime + 0.1;
        reloj = setInterval(programar, 60);
    }

    function efecto(fn) {
        if (!activo || !iniciar()) return;
        if (ctx.state === "suspended") ctx.resume();
        fn(ctx.currentTime);
    }

    function barrido(desde, hasta, inicio, duracion, tipo, volumen) {
        const osc = ctx.createOscillator();
        const env = ctx.createGain();
        osc.type = tipo;
        osc.frequency.setValueAtTime(desde, inicio);
        osc.frequency.exponentialRampToValueAtTime(hasta, inicio + duracion);
        env.gain.setValueAtTime(volumen, inicio);
        env.gain.exponentialRampToValueAtTime(0.0001, inicio + duracion);
        osc.connect(env);
        env.connect(efectos);
        osc.start(inicio);
        osc.stop(inicio + duracion + 0.05);
    }

    return {
        /** Se llama en el primer gesto del usuario (los navegadores no permiten audio antes). */
        desbloquear() {
            if (!iniciar()) return;
            if (ctx.state === "suspended") ctx.resume();
            if (activo) arrancarMusica();
        },
        alternar() {
            activo = !activo;
            guardar("odisea.sonido", activo);
            if (iniciar()) {
                maestro.gain.setTargetAtTime(activo ? 1 : 0, ctx.currentTime, 0.05);
                if (activo) { if (ctx.state === "suspended") ctx.resume(); arrancarMusica(); }
            }
            return activo;
        },
        get activo() { return activo; },
        clic: () => efecto((t) => barrido(900, 600, t, 0.06, "square", 0.12)),
        salto: () => efecto((t) => { barrido(220, 880, t, 0.28, "sine", 0.5); barrido(110, 330, t, 0.3, "triangle", 0.25); }),
        caja: () => efecto((t) => { barrido(240, 120, t, 0.16, "sine", 0.6); barrido(700, 500, t, 0.05, "square", 0.1); }),
        error: () => efecto((t) => { barrido(180, 160, t, 0.1, "square", 0.25); barrido(150, 130, t + 0.13, 0.12, "square", 0.25); }),
        fallo: () => efecto((t) => { barrido(420, 50, t, 0.9, "sawtooth", 0.35); barrido(300, 40, t + 0.1, 0.8, "square", 0.15); }),
        estrella: (i) => efecto((t) => {
            for (const [k, m] of [[0, 84], [1, 88], [2, 91]].slice(0, 1)) nota(m + i * 4, t + k * 0.05, 0.6, { tipo: "triangle", volumen: 0.35, destino: efectos, ataque: 0.005 });
            nota(96 + i * 4, t, 0.35, { tipo: "sine", volumen: 0.18, destino: efectos, ataque: 0.005 });
        }),
        exito: () => efecto((t) => [72, 76, 79, 84].forEach((m, i) => nota(m, t + i * 0.09, 0.4, { tipo: "triangle", volumen: 0.3, destino: efectos, ataque: 0.005 }))),
        despegue: () => efecto((t) => {
            const largo = 1.2;
            const buffer = ctx.createBuffer(1, ctx.sampleRate * largo, ctx.sampleRate);
            const datos = buffer.getChannelData(0);
            for (let i = 0; i < datos.length; i++) datos[i] = Math.random() * 2 - 1;
            const ruido = ctx.createBufferSource();
            ruido.buffer = buffer;
            const filtro = ctx.createBiquadFilter();
            filtro.type = "bandpass";
            filtro.frequency.setValueAtTime(200, t);
            filtro.frequency.exponentialRampToValueAtTime(3000, t + largo);
            const env = ctx.createGain();
            env.gain.setValueAtTime(0.001, t);
            env.gain.exponentialRampToValueAtTime(0.6, t + 0.3);
            env.gain.exponentialRampToValueAtTime(0.0001, t + largo);
            ruido.connect(filtro);
            filtro.connect(env);
            env.connect(efectos);
            ruido.start(t);
            barrido(80, 400, t, largo, "sawtooth", 0.15);
        })
    };
})();

/* =============================== Fondo de estrellas =============================== */

function iniciarEstrellas() {
    const lienzo = $("estrellas");
    const g = lienzo.getContext("2d");
    let puntos = [];
    function ajustar() {
        const escala = window.devicePixelRatio || 1;
        lienzo.width = innerWidth * escala;
        lienzo.height = innerHeight * escala;
        g.setTransform(escala, 0, 0, escala, 0, 0);
        puntos = Array.from({ length: 180 }, () => ({ x: Math.random() * innerWidth, y: Math.random() * innerHeight, z: 0.3 + Math.random() * 1.3, f: Math.random() * 6.3 }));
    }
    function dibujar(t) {
        g.clearRect(0, 0, innerWidth, innerHeight);
        for (const p of puntos) {
            if (!sinAnimacion) { p.x -= 0.07 * p.z; if (p.x < 0) { p.x = innerWidth; p.y = Math.random() * innerHeight; } }
            g.fillStyle = `rgba(215, 228, 255, ${sinAnimacion ? 0.7 : 0.45 + 0.4 * Math.sin(t / 900 + p.f)})`;
            g.beginPath();
            g.arc(p.x, p.y, p.z, 0, 6.3);
            g.fill();
        }
        if (!sinAnimacion) requestAnimationFrame(dibujar);
    }
    ajustar();
    addEventListener("resize", () => { ajustar(); if (sinAnimacion) dibujar(0); });
    requestAnimationFrame(dibujar);
}

/* =============================== Datos del grafo =============================== */

const datos = {
    grafo: null,
    nombre: new Map(),     // id -> nombre
    idDe: new Map(),       // nombre -> id
    vecinos: new Map(),    // id -> [{ id, costo }]
    distancia: new Map()   // id -> Map(id -> costo mínimo), de Floyd-Warshall
};
const nombre = (id) => datos.nombre.get(id) || id;
const costoRuta = (a, b) => datos.vecinos.get(a).find((v) => v.id === b).costo;

/* =============================== Niveles =============================== */

const MUNDOS = [
    {
        id: "saltos", nombre: "Saltos de emergencia", icono: "🚀", color: "#3ee6ff", algoritmo: "Dijkstra",
        descripcion: "Llegá a destino con el combustible justo.",
        niveles: [
            { id: "s1", tipo: "viaje", origen: "SOL", destino: "SIRIUS", margen: 1.6 },
            { id: "s2", tipo: "viaje", origen: "ALPHA", destino: "NOVA", margen: 1.3 },
            { id: "s3", tipo: "viaje", origen: "SOL", destino: "CITADEL", margen: 1.15 }
        ]
    },
    {
        id: "seguras", nombre: "Rutas seguras", icono: "🛡️", color: "#b49bff", algoritmo: "Backtracking",
        descripcion: "Combustible y saltos limitados a la vez.",
        niveles: [
            { id: "b1", tipo: "segura", origen: "SOL", destino: "CITADEL", maxCosto: 70, maxSaltos: 4 },
            { id: "b2", tipo: "segura", origen: "SOL", destino: "CITADEL", maxCosto: 56, maxSaltos: 5 }
        ]
    },
    {
        id: "bodega", nombre: "Bodega", icono: "📦", color: "#ffc24f", algoritmo: "Mochila 0/1",
        descripcion: "El mayor valor sin pasarte de peso.",
        niveles: [
            { id: "c1", tipo: "carga", capacidad: 10, lotes: [["Cristal de Taquiones", 6, 66], ["Núcleo de Plasma", 5, 50], ["Aleación de Titanio", 5, 50]] },
            { id: "c2", tipo: "carga", capacidad: 12, lotes: [["Lingote de Iridio", 7, 84], ["Hielo de Europa", 6, 66], ["Chip Cuántico", 6, 66], ["Polvo de Estrella", 2, 14]] },
            { id: "c3", tipo: "carga", capacidad: 20, lotes: [["Fragmento de Antimateria", 3, 45], ["Cristal de Taquiones", 9, 108], ["Lingote de Iridio", 8, 88], ["Núcleo de Plasma", 6, 60], ["Aleación de Titanio", 6, 60], ["Celdas de Helio-3", 4, 32], ["Esporas de Kepler", 5, 35]] }
        ]
    },
    {
        id: "balizas", nombre: "Red de balizas", icono: "📡", color: "#5cf29a", algoritmo: "Kruskal",
        descripcion: "Conectá todas las estaciones al menor costo.",
        niveles: [{ id: "m1", tipo: "red" }]
    },
    {
        id: "reparto", nombre: "Reparto", icono: "🧭", color: "#ff7fb0", algoritmo: "Branch & Bound",
        descripcion: "El mejor orden para entregar y volver.",
        niveles: [
            { id: "r1", tipo: "reparto", base: "SOL", paradas: ["ALPHA", "VEGA", "KEPLER"] },
            { id: "r2", tipo: "reparto", base: "SOL", paradas: ["VEGA", "NOVA", "CITADEL", "SIRIUS", "ORION"] }
        ]
    }
];
const NIVELES = MUNDOS.flatMap((m) => m.niveles.map((n, i) => ({ ...n, mundo: m, numero: i + 1 })));

let progreso = leer("odisea.progreso", {});
const estrellasDe = (id) => progreso[id] || 0;
const desbloqueado = (i) => i === 0 || estrellasDe(NIVELES[i - 1].id) > 0;

/* =============================== Mapa estelar (planetas) =============================== */

const POSICIONES = {
    SOL: [400, 62], ALPHA: [185, 155], SIRIUS: [615, 155], VEGA: [245, 275],
    KEPLER: [555, 275], ORION: [315, 390], NOVA: [625, 390], CITADEL: [470, 475]
};
// [luz, color, sombra, anillo]
const PLANETAS = {
    SOL: ["#fff7c2", "#ffb52e", "#c24a00"], ALPHA: ["#ffd2b0", "#ff7a3d", "#7a2508"],
    SIRIUS: ["#ecfbff", "#7fd6ff", "#1e4f8f"], VEGA: ["#f1d8ff", "#a46bff", "#3b1680", "#d6b8ff"],
    KEPLER: ["#d9ffd6", "#4fd17a", "#135a33"], ORION: ["#ffd9f0", "#ff6fb8", "#7a1450"],
    NOVA: ["#ffd0cf", "#ff4d5e", "#6e0b1b"], CITADEL: ["#d8fbff", "#3ee6ff", "#0b4b66", "#9ff3ff"]
};
const PALETA_EXTRA = ["#ffe08a", "#8ad1ff", "#c79bff", "#8affb8"];

function posicionDe(id, i, total) {
    if (POSICIONES[id]) return POSICIONES[id];
    const a = (2 * Math.PI * i) / total - Math.PI / 2;
    return [400 + 300 * Math.cos(a), 270 + 210 * Math.sin(a)];
}

function crearMapa(contenedor, { alElegirPlaneta, alElegirCarril } = {}) {
    const { estaciones, rutas } = datos.grafo;
    const pos = new Map(estaciones.map((e, i) => [e.id, posicionDe(e.id, i, estaciones.length)]));
    const lienzo = svg("svg", { viewBox: "0 0 800 540", role: "group", "aria-label": "Mapa estelar" });
    const defs = svg("defs");
    const capas = ["carriles", "costos", "planetas", "nave"].map(() => svg("g"));
    lienzo.append(defs, ...capas);
    const [capaCarriles, capaCostos, capaPlanetas, capaNave] = capas;

    const carriles = new Map();
    const costos = new Map();
    for (const r of rutas) {
        const [x1, y1] = pos.get(r.origen);
        const [x2, y2] = pos.get(r.destino);
        const clave = claveRuta(r.origen, r.destino);
        const linea = svg("line", { class: "carril", x1, y1, x2, y2 });
        capaCarriles.appendChild(linea);
        carriles.set(clave, linea);
        const mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
        const etiqueta = svg("g", { class: "costo" });
        etiqueta.append(svg("rect", { x: mx - 17, y: my - 12, width: 34, height: 24, rx: 8 }), svg("text", { x: mx, y: my }, r.costoCA));
        capaCostos.appendChild(etiqueta);
        costos.set(clave, etiqueta);
        if (alElegirCarril) {
            const elegir = () => alElegirCarril(r.origen, r.destino);
            const clic = svg("line", { class: "carril-clic", x1, y1, x2, y2, tabindex: 0, role: "button", "aria-label": `Carril ${nombre(r.origen)} – ${nombre(r.destino)}, ${r.costoCA}` });
            clic.addEventListener("click", elegir);
            clic.addEventListener("keydown", (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); elegir(); } });
            etiqueta.classList.add("costo--clic");
            etiqueta.addEventListener("click", elegir);
            capaCarriles.appendChild(clic);
        }
    }

    const planetas = new Map();
    estaciones.forEach((e, i) => {
        const [x, y] = pos.get(e.id);
        const [luz, color, sombra, anillo] = PLANETAS[e.id] || [ "#fff", PALETA_EXTRA[i % PALETA_EXTRA.length], "#222" ];
        const gradiente = svg("radialGradient", { id: `p-${e.id}`, cx: "35%", cy: "30%", r: "75%" });
        gradiente.append(svg("stop", { offset: "0%", "stop-color": luz }), svg("stop", { offset: "55%", "stop-color": color }), svg("stop", { offset: "100%", "stop-color": sombra }));
        const brillo = svg("radialGradient", { id: `b-${e.id}` });
        brillo.append(svg("stop", { offset: "0%", "stop-color": color, "stop-opacity": "0.8" }), svg("stop", { offset: "100%", "stop-color": color, "stop-opacity": "0" }));
        defs.append(gradiente, brillo);

        const g = svg("g", { class: "planeta", "data-id": e.id });
        g.append(svg("circle", { class: "resplandor", cx: x, cy: y, r: e.id === "SOL" ? 50 : 42, fill: `url(#b-${e.id})` }));
        if (anillo) g.append(svg("ellipse", { class: "anillo", cx: x, cy: y, rx: 38, ry: 11, stroke: anillo, transform: `rotate(-18 ${x} ${y})` }));
        g.append(svg("circle", { class: "cuerpo", cx: x, cy: y, r: e.id === "SOL" ? 28 : 23, fill: `url(#p-${e.id})` }),
            svg("text", { class: "marca", x, y }, ""),
            svg("text", { class: "objetivo-pin", x: x + 24, y: y - 26 }, ""),
            svg("text", { x, y: y + 46 }, e.nombre));
        if (alElegirPlaneta) {
            const elegir = () => { if (g.classList.contains("planeta--clic")) alElegirPlaneta(e.id); };
            g.addEventListener("click", elegir);
            g.addEventListener("keydown", (ev) => { if (ev.key === "Enter" || ev.key === " ") { ev.preventDefault(); elegir(); } });
        }
        capaPlanetas.appendChild(g);
        planetas.set(e.id, g);
    });

    const marco = crear("div", "mapa");
    marco.appendChild(lienzo);
    contenedor.replaceChildren(marco);

    const nave = svg("g", { class: "nave" });
    nave.append(svg("path", { class: "llama", d: "M-8 -3.5 L-20 0 L-8 3.5 Z" }), svg("path", { class: "casco", d: "M17 0 L-10 -10 L-5 0 L-10 10 Z" }));
    let naveVisible = false;
    const ubicar = ([x, y], angulo = 0) => nave.setAttribute("transform", `translate(${x} ${y - 30}) rotate(${angulo})`);

    return {
        planeta(id, clase, activo) { planetas.get(id)?.classList.toggle(clase, activo); },
        clicable(id, activo) {
            const g = planetas.get(id);
            if (!g) return;
            g.classList.toggle("planeta--clic", activo);
            if (activo) { g.setAttribute("tabindex", "0"); g.setAttribute("role", "button"); g.setAttribute("aria-label", `Ir a ${nombre(id)}`); }
            else { g.removeAttribute("tabindex"); g.removeAttribute("role"); g.removeAttribute("aria-label"); }
        },
        todosNoClicables() { for (const id of planetas.keys()) this.clicable(id, false); },
        marca(id, texto) { planetas.get(id).querySelector(".marca").textContent = texto ?? ""; },
        pin(id, emoji) { planetas.get(id).querySelector(".objetivo-pin").textContent = emoji ?? ""; },
        carril(a, b, clase, activo) {
            const clave = claveRuta(a, b);
            carriles.get(clave)?.classList.toggle(clase, activo);
            if (clase === "carril--usado") costos.get(clave)?.classList.toggle("costo--activo", activo);
        },
        limpiar(clase) {
            for (const [clave, linea] of carriles) { linea.classList.remove(clase); if (clase === "carril--usado") costos.get(clave).classList.remove("costo--activo"); }
        },
        ubicar(id) {
            if (!naveVisible) { capaNave.appendChild(nave); naveVisible = true; }
            nave.classList.remove("nave--averiada");
            ubicar(pos.get(id));
        },
        averiar() { nave.classList.add("nave--averiada"); },
        /** Vuela por la lista de estaciones; la promesa termina al llegar. */
        volar(ids) {
            if (!naveVisible) { capaNave.appendChild(nave); naveVisible = true; }
            nave.classList.remove("nave--averiada");
            const puntos = ids.map((id) => pos.get(id));
            if (sinAnimacion || puntos.length < 2) { ubicar(puntos[puntos.length - 1]); return Promise.resolve(); }
            return new Promise((fin) => {
                let tramo = 0, inicio = null;
                function cuadro(t) {
                    const [x1, y1] = puntos[tramo], [x2, y2] = puntos[tramo + 1];
                    const duracion = Math.max(320, Math.hypot(x2 - x1, y2 - y1) * 2.2);
                    if (inicio === null) inicio = t;
                    const p = Math.min(1, (t - inicio) / duracion);
                    const s = p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2;
                    ubicar([x1 + (x2 - x1) * s, y1 + (y2 - y1) * s], Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI);
                    if (p < 1) return requestAnimationFrame(cuadro);
                    tramo++; inicio = null;
                    if (tramo < puntos.length - 1) requestAnimationFrame(cuadro); else fin();
                }
                requestAnimationFrame(cuadro);
            });
        }
    };
}

/* =============================== Tipos de nivel =============================== */

/** Viaje de A a B (Dijkstra) o con doble límite (Backtracking). */
function nivelViaje(def) {
    const conLimites = def.tipo === "segura";
    let mapa, camino, gastado, tanque, optimo, rutasValidas, metricas;
    let ocupado = false;

    const actual = () => camino[camino.length - 1];
    const saltos = () => camino.length - 1;
    const disponibles = () => actual() === def.destino ? []
        : datos.vecinos.get(actual()).filter((v) => !camino.includes(v.id)).map((v) => v.id);

    function refrescar() {
        mapa.todosNoClicables();
        mapa.limpiar("carril--disponible");
        mapa.limpiar("carril--usado");
        for (let i = 0; i + 1 < camino.length; i++) mapa.carril(camino[i], camino[i + 1], "carril--usado", true);
        for (const id of datos.nombre.keys()) mapa.planeta(id, "planeta--visitado", camino.includes(id) && id !== def.origen);
        if (!ocupado) {
            for (const id of disponibles()) { mapa.clicable(id, true); mapa.carril(actual(), id, "carril--disponible", true); }
        }
        refrescarHud();
    }

    function solucion() {
        const ids = conLimites ? rutasValidas[0].caminoIds : optimo.caminoIds;
        mapa.limpiar("carril--disponible");
        for (let i = 0; i + 1 < ids.length; i++) mapa.carril(ids[i], ids[i + 1], "carril--solucion", true);
        mapa.volar(ids);
    }

    async function saltar(id) {
        if (ocupado) return;
        ocupado = true;
        const desde = actual();
        camino.push(id);
        gastado += costoRuta(desde, id);
        sonido.salto();
        refrescar();
        await mapa.volar([desde, id]);
        ocupado = false;

        const sinCombustible = gastado > (conLimites ? def.maxCosto : tanque);
        const sinSaltos = conLimites && saltos() > def.maxSaltos;
        if (sinCombustible || sinSaltos) {
            mapa.averiar();
            sonido.fallo();
            await esperar(700);
            return terminar({
                exito: false,
                titulo: sinCombustible ? "¡Tanque vacío!" : "¡Motores recalentados!",
                texto: sinCombustible
                    ? `Gastaste ${gastado} CA y el límite era ${conLimites ? def.maxCosto : tanque}. Probá otra ruta.`
                    : `Hiciste ${saltos()} saltos y los motores aguantan ${def.maxSaltos}.`,
                solucion
            });
        }
        if (id === def.destino) {
            const mejor = conLimites ? rutasValidas[0].costoTotal : optimo.consumoTotal;
            return terminar({
                exito: true,
                estrellas: calificar(mejor / gastado),
                titulo: gastado === mejor ? "¡Ruta perfecta!" : "¡Llegaste!",
                texto: conLimites
                    ? `Había ${rutasValidas.length} rutas que cumplían los dos límites. El backtracking las encontró explorando ${metricas.nodosExploradosConPoda} nodos en lugar de ${metricas.nodosExploradosSinPoda}, gracias a la poda.`
                    : `Dijkstra encontró la ruta más barata expandiendo siempre la estación más cercana, con una cola de prioridad.`,
                comparacion: [["Tu consumo", `${gastado} CA`, "jugador"], [conLimites ? "Mejor ruta válida" : "Dijkstra", `${mejor} CA`, "maquina"]],
                solucion
            });
        }
        refrescar();
    }

    return {
        objetivo: conLimites
            ? `Llevá la nave de ${nombre(def.origen)} a ${nombre(def.destino)} con hasta ${def.maxCosto} CA y ${def.maxSaltos} saltos.`
            : `Llevá la nave de ${nombre(def.origen)} a ${nombre(def.destino)} sin vaciar el tanque.`,
        async preparar() {
            if (conLimites) {
                const r = await api(`/api/navegacion/rutas-seguras?origen=${def.origen}&destino=${def.destino}&maxCosto=${def.maxCosto}&maxSaltos=${def.maxSaltos}`);
                rutasValidas = r.rutasValidas;
                metricas = r.metricasExploracion;
            } else {
                optimo = await api(`/api/navegacion/dijkstra?origen=${def.origen}&destino=${def.destino}`);
                tanque = Math.ceil(optimo.consumoTotal * def.margen);
            }
        },
        montar(escena) {
            camino = [def.origen];
            gastado = 0;
            mapa = crearMapa(escena, { alElegirPlaneta: saltar });
            mapa.planeta(def.destino, "planeta--destino", true);
            mapa.pin(def.destino, "🏁");
            mapa.ubicar(def.origen);
            refrescar();
        },
        indicadores() {
            const limite = conLimites ? def.maxCosto : tanque;
            const resto = Math.max(0, limite - gastado);
            const lista = [{ nombre: "Combustible", valor: `${resto} / ${limite} CA`, barra: resto / limite, bajo: resto / limite < 0.25 }];
            if (conLimites) lista.push({ nombre: "Saltos disponibles", valor: `${Math.max(0, def.maxSaltos - saltos())} / ${def.maxSaltos}`, barra: Math.max(0, def.maxSaltos - saltos()) / def.maxSaltos, bajo: def.maxSaltos - saltos() <= 1 });
            return lista;
        },
        ayuda() {
            if (camino.length === 1) return conLimites
                ? "Cada salto suma su consumo y además gasta un salto. Si te pasás de cualquiera de los dos límites, la nave falla."
                : "Tocá un planeta que brille para saltar. El número de cada carril es lo que consume. Lo directo no siempre es lo más barato.";
            if (disponibles().length === 0) return "Sin salida: deshacé el último salto (↶).";
            return "Seguí saltando. Si te equivocás, podés deshacer.";
        },
        puedeDeshacer: () => !ocupado && camino.length > 1,
        deshacer() {
            const desde = camino.pop();
            gastado -= costoRuta(actual(), desde);
            mapa.ubicar(actual());
            refrescar();
        }
    };
}

/** Elegir cajas para la bodega (Mochila 0/1; también se compara con el Greedy). */
function nivelCarga(def) {
    const COLORES = ["#ff7a3d", "#3ee6ff", "#b49bff", "#5cf29a", "#ffc24f", "#ff6fb8", "#7fd6ff"];
    const ICONOS = { "Cristal de Taquiones": "💎", "Núcleo de Plasma": "🔮", "Aleación de Titanio": "🔩", "Fragmento de Antimateria": "⚛️", "Lingote de Iridio": "🪙", "Celdas de Helio-3": "🔋", "Polvo de Estrella": "✨", "Hielo de Europa": "🧊", "Esporas de Kepler": "🧬", "Chip Cuántico": "💾" };
    const lotes = def.lotes.map(([n, peso, valor], i) => ({ nombre: n, peso, valor, color: COLORES[i % COLORES.length], icono: ICONOS[n] || "📦" }));
    let cargados = [];
    let cajas, casco, boton, contenedorNave;
    let cerrado = false;

    const peso = () => cargados.reduce((s, l) => s + l.peso, 0);
    const valor = () => cargados.reduce((s, l) => s + l.valor, 0);

    function refrescar() {
        const libre = def.capacidad - peso();
        for (const l of lotes) {
            const caja = cajas.get(l.nombre);
            const aBordo = cargados.includes(l);
            caja.style.visibility = aBordo ? "hidden" : "visible";
            caja.disabled = cerrado || l.peso > libre;
        }
        casco.replaceChildren(...cargados.map((l) => {
            const pieza = crear("button", "carga-apilada", `${l.icono} ${l.peso} t`);
            pieza.type = "button";
            pieza.title = `Bajar ${l.nombre}`;
            pieza.style.setProperty("--color-caja", l.color);
            pieza.style.height = `${(l.peso / def.capacidad) * 226}px`;
            pieza.style.border = "0";
            pieza.style.cursor = cerrado ? "default" : "pointer";
            pieza.addEventListener("click", () => { if (cerrado) return; cargados = cargados.filter((x) => x !== l); sonido.clic(); refrescar(); });
            return pieza;
        }));
        boton.disabled = cerrado || cargados.length === 0;
        refrescarHud();
    }

    async function despegar() {
        cerrado = true;
        refrescar();
        sonido.despegue();
        contenedorNave.classList.add("nave-bodega--despegue");
        const cuerpo = { capacidadBodega: def.capacidad, itemsDisponibles: lotes.map(({ nombre: n, peso: p, valor: v }) => ({ nombre: n, peso: p, valor: v })) };
        let optimo, voraz;
        try {
            [optimo, voraz] = await Promise.all([enviarJson("/api/bodega/cargar-optimo", cuerpo), enviarJson("/api/bodega/cargar-greedy", cuerpo)]);
        } catch (e) {
            cerrado = false;
            contenedorNave.classList.remove("nave-bodega--despegue");
            refrescar();
            return avisar(e.message);
        }
        await esperar(1100);
        const mio = valor();
        const mejor = optimo.valorTotalObtenido;
        const leGanoAlVoraz = mio > voraz.valorTotalObtenido;
        terminar({
            exito: true,
            estrellas: calificar(mio / mejor),
            titulo: mio === mejor ? "¡Carga perfecta!" : "¡Despegaste!",
            texto: (leGanoAlVoraz ? "¡Le ganaste al algoritmo voraz! " : "")
                + (voraz.valorTotalObtenido < mejor
                    ? "Llenar primero lo de mejor valor por tonelada no alcanzaba: la programación dinámica prueba cada caja contra cada capacidad."
                    : "Acá elegir por mejor valor por tonelada sí llegaba al óptimo, y la programación dinámica lo confirma."),
            comparacion: [["Tu carga", `${mio} CG`, "jugador"], ["Óptimo (DP)", `${numero(mejor)} CG`, "maquina"], ["Voraz", `${numero(voraz.valorTotalObtenido)} CG`, ""]],
            solucion() {
                contenedorNave.classList.remove("nave-bodega--despegue");
                cargados = optimo.mineralesCargados.map((m) => lotes.find((l) => l.nombre === m.nombre));
                refrescar();
                for (const l of cargados) cajas.get(l.nombre).classList.add("caja--solucion");
            }
        });
    }

    return {
        objetivo: `Cargá el mayor valor posible sin pasarte de ${def.capacidad} toneladas.`,
        montar(escena) {
            const zona = crear("div", "bodega");
            const muelle = crear("div", "muelle");
            cajas = new Map(lotes.map((l) => {
                const caja = crear("button", "caja caja--entrando");
                caja.type = "button";
                caja.style.setProperty("--color-caja", l.color);
                caja.style.setProperty("--alto", `${54 + l.peso * 13}px`);
                caja.setAttribute("aria-label", `${l.nombre}: ${l.peso} toneladas, ${l.valor} créditos`);
                caja.append(crear("span", "caja__icono", l.icono), crear("span", "caja__nombre", l.nombre), crear("span", "caja__datos", `${l.peso} t · ${l.valor} CG`));
                caja.addEventListener("click", () => {
                    if (cerrado || cargados.includes(l)) return;
                    cargados = [...cargados, l];
                    sonido.caja();
                    refrescar();
                });
                muelle.appendChild(caja);
                return [l.nombre, caja];
            }));
            contenedorNave = crear("div", "nave-bodega");
            casco = crear("div", "nave-bodega__casco");
            boton = crear("button", "boton boton--despegar", "Despegar 🚀");
            boton.type = "button";
            boton.addEventListener("click", despegar);
            contenedorNave.append(casco, crear("span", "nave-bodega__limite", `Bodega: ${def.capacidad} t`), boton);
            zona.append(muelle, contenedorNave);
            escena.replaceChildren(zona);
            refrescar();
        },
        indicadores: () => [
            { nombre: "Peso a bordo", valor: `${peso()} / ${def.capacidad} t`, barra: peso() / def.capacidad, ok: peso() === def.capacidad },
            { nombre: "Valor de la carga", valor: `${valor()} CG` }
        ],
        ayuda: () => cargados.length === 0
            ? "Tocá una caja para subirla a la nave; tocala adentro para bajarla. Las que ya no entran se apagan."
            : "Cuando estés conforme, despegá. Ojo: lo que más vale por tonelada no siempre es lo mejor.",
        puedeDeshacer: () => !cerrado && cargados.length > 0,
        deshacer() { cargados.pop(); refrescar(); }
    };
}

/** Tender balizas hasta conectar todas las estaciones (Kruskal). */
function nivelRed() {
    const ids = () => [...datos.nombre.keys()];
    let mapa, elegidos;
    let cerrado = false;

    const costo = () => elegidos.reduce((s, c) => { const [a, b] = c.split("|"); return s + costoRuta(a, b); }, 0);
    function conectados(a, b) {
        const ady = new Map(ids().map((id) => [id, []]));
        for (const c of elegidos) { const [x, y] = c.split("|"); ady.get(x).push(y); ady.get(y).push(x); }
        const visto = new Set([a]);
        const pila = [a];
        while (pila.length) for (const w of ady.get(pila.pop())) if (!visto.has(w)) { visto.add(w); pila.push(w); }
        return visto.has(b);
    }

    function refrescar() {
        mapa.limpiar("carril--usado");
        const unidas = new Set();
        for (const c of elegidos) { const [a, b] = c.split("|"); mapa.carril(a, b, "carril--usado", true); unidas.add(a); unidas.add(b); }
        for (const id of ids()) mapa.planeta(id, "planeta--conectado", unidas.has(id));
        refrescarHud();
    }

    async function elegir(a, b) {
        if (cerrado) return;
        const clave = claveRuta(a, b);
        if (elegidos.includes(clave)) {
            elegidos = elegidos.filter((c) => c !== clave);
            sonido.clic();
            return refrescar();
        }
        if (conectados(a, b)) {
            sonido.error();
            mapa.carril(a, b, "carril--alerta", true);
            ayudaExtra = `${nombre(a)} y ${nombre(b)} ya están comunicadas: esa baliza cerraría un ciclo.`;
            refrescarHud();
            await esperar(600);
            mapa.carril(a, b, "carril--alerta", false);
            return;
        }
        ayudaExtra = null;
        elegidos = [...elegidos, clave];
        sonido.caja();
        refrescar();
        if (elegidos.length === ids().length - 1) {
            cerrado = true;
            let r;
            try {
                r = await api("/api/red/mst?metodo=kruskal");
            } catch (e) {
                cerrado = false;
                return avisar(e.message);
            }
            const mio = costo();
            terminar({
                exito: true,
                estrellas: calificar(r.costoTotalMST / mio),
                titulo: mio === r.costoTotalMST ? "¡Red perfecta!" : "¡Red conectada!",
                texto: "Kruskal ordena los carriles del más barato al más caro y tiende cada uno salvo que cierre un ciclo (lo detecta con Union-Find).",
                comparacion: [["Tu red", `${mio} CA`, "jugador"], ["Kruskal", `${r.costoTotalMST} CA`, "maquina"]],
                solucion() {
                    for (const arista of r.aristasSeleccionadas) mapa.carril(datos.idDe.get(arista.origen), datos.idDe.get(arista.destino), "carril--solucion", true);
                }
            });
        }
    }

    let ayudaExtra = null;
    return {
        objetivo: "Tendé balizas en los carriles hasta comunicar las ocho estaciones, gastando lo menos posible.",
        montar(escena) {
            elegidos = [];
            mapa = crearMapa(escena, { alElegirCarril: elegir });
            refrescar();
        },
        indicadores: () => [
            { nombre: "Balizas", valor: `${elegidos.length} / ${ids().length - 1}`, barra: elegidos.length / (ids().length - 1) },
            { nombre: "Costo de la red", valor: `${costo()} CA` }
        ],
        ayuda: () => ayudaExtra || (elegidos.length === 0
            ? "Tocá un carril (la línea o su número) para tender una baliza. Con 7 balizas bien puestas quedan las 8 estaciones comunicadas."
            : "Tocá una baliza tendida para quitarla."),
        puedeDeshacer: () => !cerrado && elegidos.length > 0,
        deshacer() { elegidos.pop(); ayudaExtra = null; refrescar(); }
    };
}

/** Ordenar las entregas y volver a la base (Branch & Bound). */
function nivelReparto(def) {
    let mapa, orden, gastado;
    let ocupado = false;
    const distancia = (a, b) => datos.distancia.get(a).get(b);
    const ultimo = () => (orden.length ? orden[orden.length - 1] : def.base);

    function refrescar() {
        mapa.todosNoClicables();
        for (const id of def.paradas) {
            const posicion = orden.indexOf(id);
            mapa.marca(id, posicion >= 0 ? String(posicion + 1) : "");
            mapa.pin(id, posicion >= 0 ? "✅" : "📦");
            mapa.planeta(id, "planeta--visitado", posicion >= 0);
            if (!ocupado && posicion < 0) mapa.clicable(id, true);
        }
        refrescarHud();
    }

    async function volarPorCaminoMinimo(desde, hasta) {
        const r = await api(`/api/navegacion/dijkstra?origen=${desde}&destino=${hasta}`);
        for (let i = 0; i + 1 < r.caminoIds.length; i++) mapa.carril(r.caminoIds[i], r.caminoIds[i + 1], "carril--usado", true);
        sonido.salto();
        await mapa.volar(r.caminoIds);
    }

    async function visitar(id) {
        if (ocupado) return;
        ocupado = true;
        const desde = ultimo();
        orden.push(id);
        gastado += distancia(desde, id);
        refrescar();
        try {
            await volarPorCaminoMinimo(desde, id);
            if (orden.length === def.paradas.length) {
                gastado += distancia(id, def.base);
                refrescarHud();
                await volarPorCaminoMinimo(id, def.base);
                const r = await enviarJson(`/api/logistica/reparto-optimo?origen=${def.base}`, def.paradas);
                const m = r.metricas;
                return terminar({
                    exito: true,
                    estrellas: calificar(r.costoMinimoTotal / gastado),
                    titulo: gastado === r.costoMinimoTotal ? "¡Reparto perfecto!" : "¡Entregas completas!",
                    texto: `Branch & Bound probó ${numero(m.nodosGenerados)} de ${numero(m.nodosArbolCompleto)} recorridos posibles: descartó ramas enteras con una cota optimista (${m.podaEfectiva} de poda).`,
                    comparacion: [["Tu recorrido", `${gastado} CA`, "jugador"], ["Branch & Bound", `${r.costoMinimoTotal} CA`, "maquina"]],
                    solucion() {
                        mapa.limpiar("carril--usado");
                        const ids = r.recorridoCompleto.map((n) => datos.idDe.get(n));
                        for (let i = 0; i + 1 < ids.length; i++) mapa.carril(ids[i], ids[i + 1], "carril--solucion", true);
                        r.recorridoOptimo.slice(1, -1).forEach((n, i) => mapa.marca(datos.idDe.get(n), String(i + 1)));
                        mapa.volar(ids);
                    }
                });
            }
        } catch (e) {
            avisar(e.message);
        } finally {
            ocupado = false;
        }
        refrescar();
    }

    return {
        objetivo: `Entregá en las ${def.paradas.length} estaciones marcadas con 📦 y volvé a ${nombre(def.base)} gastando lo menos posible.`,
        montar(escena) {
            orden = [];
            gastado = 0;
            mapa = crearMapa(escena, { alElegirPlaneta: visitar });
            mapa.pin(def.base, "🏠");
            for (const id of def.paradas) mapa.planeta(id, "planeta--parada", true);
            mapa.ubicar(def.base);
            refrescar();
        },
        indicadores: () => [
            { nombre: "Entregas", valor: `${orden.length} / ${def.paradas.length}`, barra: orden.length / def.paradas.length },
            { nombre: "Combustible usado", valor: `${gastado} CA` }
        ],
        ayuda: () => orden.length === 0
            ? "Tocá las estaciones con 📦 en el orden en que querés visitarlas. Entre una y otra la nave toma el camino más barato. Al final vuelve sola a la base."
            : "Elegí la próxima entrega.",
        puedeDeshacer: () => !ocupado && orden.length > 0 && orden.length < def.paradas.length,
        deshacer() {
            const id = orden.pop();
            gastado -= distancia(ultimo(), id);
            mapa.limpiar("carril--usado");
            mapa.ubicar(ultimo());
            refrescar();
        }
    };
}

const TIPOS = { viaje: nivelViaje, segura: nivelViaje, carga: nivelCarga, red: nivelRed, reparto: nivelReparto };

/* =============================== Pantallas =============================== */

let nivelActual = -1;
let controlador = null;
let resultado = null;

function mostrar(vista) {
    $("vistaMapa").hidden = vista !== "mapa";
    $("vistaNivel").hidden = vista !== "nivel";
    window.scrollTo({ top: 0 });
}

function actualizarTotal() {
    const total = NIVELES.reduce((s, n) => s + estrellasDe(n.id), 0);
    $("totalEstrellas").textContent = total;
    $("totalMaximo").textContent = ` / ${NIVELES.length * 3}`;
}

function estrellitas(n) {
    const caja = crear("span", "nivel-boton__estrellas");
    for (let i = 0; i < 3; i++) caja.appendChild(crear("span", i < n ? "on" : "off", "★"));
    return caja;
}

function dibujarMapaDeNiveles() {
    actualizarTotal();
    const siguiente = NIVELES.findIndex((n, i) => desbloqueado(i) && estrellasDe(n.id) === 0);
    $("mundos").replaceChildren(...MUNDOS.map((m) => {
        const tarjeta = crear("section", "mundo");
        tarjeta.style.setProperty("--color-mundo", m.color);
        const cabecera = crear("div", "mundo__cabecera");
        const ganadas = m.niveles.reduce((s, n) => s + estrellasDe(n.id), 0);
        const texto = crear("div");
        texto.append(crear("h2", null, m.nombre), crear("p", null, `${m.descripcion} · ${m.algoritmo}`));
        cabecera.append(crear("span", "mundo__icono", m.icono), texto, crear("span", "mundo__estrellas", `★ ${ganadas}/${m.niveles.length * 3}`));
        const fila = crear("div", "niveles");
        m.niveles.forEach((n, k) => {
            const i = NIVELES.findIndex((x) => x.id === n.id);
            if (k > 0) fila.appendChild(crear("span", "niveles__conector"));
            const boton = crear("button", "nivel-boton" + (i === siguiente ? " nivel-boton--siguiente" : ""), desbloqueado(i) ? String(k + 1) : "🔒");
            boton.type = "button";
            boton.disabled = !desbloqueado(i) || !datos.grafo;
            boton.setAttribute("aria-label", `${m.nombre}, nivel ${k + 1}${desbloqueado(i) ? `, ${estrellasDe(n.id)} estrellas` : ", bloqueado"}`);
            boton.appendChild(estrellitas(estrellasDe(n.id)));
            boton.addEventListener("click", () => { sonido.clic(); abrirNivel(i); });
            fila.appendChild(boton);
        });
        tarjeta.append(cabecera, fila);
        return tarjeta;
    }));
}

function refrescarHud() {
    if (!controlador) return;
    $("indicadores").replaceChildren(...controlador.indicadores().map((d) => {
        const caja = crear("div", "indicador" + (d.bajo ? " indicador--bajo" : "") + (d.ok ? " indicador--ok" : ""));
        const fila = crear("div", "indicador__fila");
        fila.append(crear("span", "indicador__nombre", d.nombre), crear("span", "indicador__valor", d.valor));
        caja.appendChild(fila);
        if (d.barra !== undefined) {
            const barra = crear("div", "indicador__barra");
            const relleno = crear("div", "indicador__relleno");
            relleno.style.width = `${Math.max(0, Math.min(1, d.barra)) * 100}%`;
            barra.appendChild(relleno);
            caja.appendChild(barra);
        }
        return caja;
    }));
    $("burbujaTexto").textContent = controlador.ayuda();
    $("btnDeshacer").disabled = !controlador.puedeDeshacer();
}

async function abrirNivel(i) {
    const def = NIVELES[i];
    nivelActual = i;
    resultado = null;
    $("resultado").hidden = true;
    $("btnOcultarSolucion").hidden = true;
    $("nivelSector").textContent = `${def.mundo.icono} ${def.mundo.nombre} · Nivel ${def.numero}`;
    $("nivelNombre").textContent = `vs. ${def.mundo.algoritmo}`;
    $("indicadores").replaceChildren();
    $("escena").replaceChildren(crear("p", "estado-carga", "Preparando el nivel…"));
    $("burbujaTexto").textContent = "Calculando…";
    mostrar("nivel");

    controlador = TIPOS[def.tipo](def);
    $("nivelObjetivo").textContent = controlador.objetivo;
    try {
        if (controlador.preparar) await controlador.preparar();
    } catch (e) {
        avisar(e.message);
        controlador = null;
        return volverAlMapa();
    }
    if (nivelActual !== i) return; // el jugador ya salió
    controlador.montar($("escena"));
    refrescarHud();
}

function terminar(r) {
    resultado = r;
    const def = NIVELES[nivelActual];
    $("btnDeshacer").disabled = true;
    if (r.exito && r.estrellas > estrellasDe(def.id)) {
        progreso = { ...progreso, [def.id]: r.estrellas };
        guardar("odisea.progreso", progreso);
    }
    actualizarTotal();
    mostrarResultado();
}

function mostrarResultado() {
    const r = resultado;
    const estrellas = $("resultadoEstrellas");
    if (r.exito) {
        estrellas.replaceChildren(...[0, 1, 2].map((k) => {
            const s = crear("span", k < r.estrellas ? "on" : "", "★");
            s.style.animationDelay = `${0.15 + k * 0.28}s`;
            return s;
        }));
        sonido.exito();
        for (let k = 0; k < r.estrellas; k++) setTimeout(() => sonido.estrella(k), 300 + k * 280);
    } else {
        estrellas.replaceChildren(crear("span", "resultado__icono-fallo", "💥"));
    }
    $("resultadoTitulo").textContent = r.titulo;
    $("resultadoTexto").textContent = r.texto;
    $("resultadoComparacion").replaceChildren(...(r.comparacion || []).map(([etiqueta, valor, tipo]) => {
        const d = crear("div", "dato" + (tipo ? ` dato--${tipo}` : ""));
        d.append(crear("small", null, etiqueta), crear("strong", null, valor));
        return d;
    }));
    const haySiguiente = nivelActual + 1 < NIVELES.length && desbloqueado(nivelActual + 1);
    $("btnSiguiente").hidden = !haySiguiente && !(r.exito && nivelActual + 1 === NIVELES.length);
    $("btnSiguiente").textContent = nivelActual + 1 === NIVELES.length ? "Ver el mapa" : "Siguiente nivel";
    $("btnSolucion").hidden = !r.solucion;
    $("resultado").hidden = false;
    $("btnOcultarSolucion").hidden = true;
    ($("btnSiguiente").hidden ? $("btnReintentar") : $("btnSiguiente")).focus({ preventScroll: true });
}

function verSolucion() {
    $("resultado").hidden = true;
    $("btnOcultarSolucion").hidden = false;
    $("burbujaTexto").textContent = "Así lo resolvió la computadora de a bordo.";
    resultado.solucion();
    resultado.solucion = null; // dibujarla una sola vez
}

function volverAlMapa() {
    nivelActual = -1;
    controlador = null;
    $("resultado").hidden = true;
    $("btnOcultarSolucion").hidden = true;
    dibujarMapaDeNiveles();
    mostrar("mapa");
}

/* =============================== Inicio =============================== */

async function cargarDatos() {
    const estado = $("estadoCarga");
    estado.hidden = false;
    estado.className = "estado-carga";
    estado.textContent = "Conectando con la base estelar…";
    try {
        const [grafo, fw] = await Promise.all([api("/api/grafo"), api("/api/navegacion/todos-contra-todos")]);
        if (!grafo.estaciones.length) throw new Error("La base no tiene estaciones cargadas.");
        datos.grafo = grafo;
        datos.nombre = new Map(grafo.estaciones.map((e) => [e.id, e.nombre]));
        datos.idDe = new Map(grafo.estaciones.map((e) => [e.nombre, e.id]));
        datos.vecinos = new Map(grafo.estaciones.map((e) => [e.id, []]));
        for (const r of grafo.rutas) {
            datos.vecinos.get(r.origen).push({ id: r.destino, costo: r.costoCA });
            datos.vecinos.get(r.destino).push({ id: r.origen, costo: r.costoCA });
        }
        datos.distancia = new Map(fw.estaciones.map((e, i) => [e.id, new Map(fw.estaciones.map((f, j) => [f.id, fw.distancias[i][j] ?? Infinity]))]));
        estado.hidden = true;
    } catch (e) {
        estado.className = "estado-carga estado-carga--error";
        estado.textContent = `No se pudo cargar el mapa: ${e.message} Tocá acá para reintentar.`;
        estado.onclick = () => cargarDatos().then(dibujarMapaDeNiveles);
    }
    dibujarMapaDeNiveles();
}

function pintarBotonSonido() {
    $("btnSonido").textContent = sonido.activo ? "🔊" : "🔇";
    $("btnSonido").setAttribute("aria-pressed", String(sonido.activo));
}

document.addEventListener("DOMContentLoaded", () => {
    iniciarEstrellas();
    pintarBotonSonido();
    // El audio solo puede arrancar después de un gesto del usuario
    const desbloquear = () => { sonido.desbloquear(); removeEventListener("pointerdown", desbloquear); removeEventListener("keydown", desbloquear); };
    addEventListener("pointerdown", desbloquear);
    addEventListener("keydown", desbloquear);

    $("btnSonido").addEventListener("click", () => { sonido.alternar(); pintarBotonSonido(); });
    $("btnLogo").addEventListener("click", volverAlMapa);
    $("btnVolver").addEventListener("click", volverAlMapa);
    $("btnDeshacer").addEventListener("click", () => { if (controlador?.puedeDeshacer()) { sonido.clic(); controlador.deshacer(); } });
    $("btnReiniciar").addEventListener("click", () => { sonido.clic(); abrirNivel(nivelActual); });
    $("btnReintentar").addEventListener("click", () => abrirNivel(nivelActual));
    $("btnSiguiente").addEventListener("click", () => (nivelActual + 1 < NIVELES.length ? abrirNivel(nivelActual + 1) : volverAlMapa()));
    $("btnSolucion").addEventListener("click", verSolucion);
    $("btnOcultarSolucion").addEventListener("click", mostrarResultado);

    cargarDatos();
});
