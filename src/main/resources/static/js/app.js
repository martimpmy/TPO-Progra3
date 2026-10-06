// Odisea Galáctica — consola web. Solo consume la API REST: ningún algoritmo corre en el navegador.
"use strict";

const $ = (id) => document.getElementById(id);
const SVG_NS = "http://www.w3.org/2000/svg";
const sinAnimacion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

const estado = {
    grafo: { estaciones: [], rutas: [] },
    origen: null,
    minerales: [],
    animacion: 0 // se incrementa para cancelar un recorrido en curso
};

/* ============================ Utilidades ============================ */

/** Llama a la API y devuelve el JSON. Si la respuesta es un error, lanza Error con el mensaje del servidor. */
async function api(ruta, opciones = {}) {
    let respuesta;
    try {
        respuesta = await fetch(ruta, opciones);
    } catch (e) {
        throw new Error("No se pudo conectar con el servidor. ¿Está corriendo la aplicación?");
    }
    const texto = await respuesta.text();
    let datos = null;
    try { datos = texto ? JSON.parse(texto) : null; } catch (e) { /* respuesta sin JSON */ }
    if (!respuesta.ok) {
        const error = new Error((datos && datos.mensaje) || `Error ${respuesta.status}`);
        error.codigo = respuesta.status;
        throw error;
    }
    return datos;
}

const enviarJson = (ruta, cuerpo) => api(ruta, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(cuerpo)
});

const numero = (n) => Number(n).toLocaleString("es-AR", { maximumFractionDigits: 2 });

function avisar(mensaje, tipo = "error") {
    const aviso = document.createElement("div");
    aviso.className = `aviso aviso--${tipo}`;
    aviso.textContent = mensaje;
    $("avisos").appendChild(aviso);
    setTimeout(() => aviso.remove(), tipo === "error" ? 6000 : 3000);
}

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

/** Deshabilita un botón y cambia su texto mientras dura una operación. */
async function conBotonOcupado(boton, textoOcupado, tarea) {
    const original = boton.textContent;
    boton.disabled = true;
    boton.textContent = textoOcupado;
    try {
        return await tarea();
    } finally {
        boton.disabled = false;
        boton.textContent = original;
    }
}

/* ============================ Pestañas ============================ */

function iniciarPestanias() {
    const pestanias = [...document.querySelectorAll('.pestania[role="tab"]')];
    const activar = (pestania) => {
        pestanias.forEach((p) => {
            const activa = p === pestania;
            p.setAttribute("aria-selected", activa);
            p.tabIndex = activa ? 0 : -1;
            $(p.getAttribute("aria-controls")).hidden = !activa;
        });
    };
    pestanias.forEach((p, i) => {
        p.addEventListener("click", () => activar(p));
        p.addEventListener("keydown", (e) => {
            if (e.key !== "ArrowRight" && e.key !== "ArrowLeft") return;
            const destino = pestanias[(i + (e.key === "ArrowRight" ? 1 : pestanias.length - 1)) % pestanias.length];
            activar(destino);
            destino.focus();
        });
    });
}

/* ============================ Estado de la base ============================ */

async function consultarEstado() {
    const boton = $("estadoBase");
    boton.className = "estado estado--cargando";
    $("estadoBaseTexto").textContent = "Conectando con AuraDB…";
    try {
        const r = await api("/api/grafo/resumen");
        boton.className = "estado estado--ok";
        $("estadoBaseTexto").textContent = `AuraDB en línea · ${r.vertices} estaciones · ${r.aristas} rutas`;
    } catch (e) {
        boton.className = "estado estado--error";
        $("estadoBaseTexto").textContent = "AuraDB sin conexión · reintentar";
    }
}

/* ============================ Mapa estelar ============================ */

// Posiciones del diagrama del informe; una estación que no esté acá se ubica en círculo.
const POSICIONES = {
    SOL: [400, 55], ALPHA: [185, 150], SIRIUS: [615, 150], VEGA: [245, 270],
    KEPLER: [555, 270], ORION: [315, 385], NOVA: [625, 385], CITADEL: [470, 470]
};

function posicionDe(id, indice, total) {
    if (POSICIONES[id]) return POSICIONES[id];
    const angulo = (2 * Math.PI * indice) / total - Math.PI / 2;
    return [400 + 300 * Math.cos(angulo), 270 + 210 * Math.sin(angulo)];
}

async function cargarMapa() {
    const contenedor = $("mapaContenedor");
    try {
        estado.grafo = await api("/api/grafo");
    } catch (e) {
        contenedor.replaceChildren(crear("p", "vacio", `No se pudo leer el mapa: ${e.message}`));
        $("mapaResumen").textContent = "sin datos";
        $("selectOrigen").disabled = true;
        $("btnRecorrer").disabled = true;
        return;
    }

    const { estaciones, rutas } = estado.grafo;
    $("mapaResumen").textContent = `${estaciones.length} estaciones · ${rutas.length} rutas`;
    if (estaciones.length === 0) {
        contenedor.replaceChildren(crear("p", "vacio", "La base no tiene estaciones cargadas."));
        return;
    }

    const pos = {};
    estaciones.forEach((e, i) => { pos[e.id] = posicionDe(e.id, i, estaciones.length); });

    const lienzo = svg("svg", { viewBox: "0 0 800 530", role: "group", "aria-label": "Mapa de estaciones y rutas" });

    for (const r of rutas) {
        const [x1, y1] = pos[r.origen];
        const [x2, y2] = pos[r.destino];
        lienzo.appendChild(svg("line", { class: "ruta", x1, y1, x2, y2 }));
    }
    // Los costos van después de todas las líneas para que ninguna los tape
    for (const r of rutas) {
        const mx = (pos[r.origen][0] + pos[r.destino][0]) / 2;
        const my = (pos[r.origen][1] + pos[r.destino][1]) / 2;
        lienzo.appendChild(svg("rect", { class: "ruta-costo-fondo", x: mx - 15, y: my - 11, width: 30, height: 22, rx: 6 }));
        lienzo.appendChild(svg("text", { class: "ruta-costo", x: mx, y: my }, r.costoCA));
    }
    for (const e of estaciones) {
        const [x, y] = pos[e.id];
        const grupo = svg("g", { class: "estacion", "data-id": e.id, tabindex: 0, role: "button", "aria-label": `Elegir ${e.nombre} como origen` });
        grupo.appendChild(svg("circle", { cx: x, cy: y, r: 27 }));
        grupo.appendChild(svg("text", { class: "estacion__id", x, y, "dominant-baseline": "central" }, e.id));
        grupo.appendChild(svg("text", { x, y: y + 45 }, e.nombre));
        grupo.addEventListener("click", () => elegirOrigen(e.id));
        grupo.addEventListener("keydown", (ev) => {
            if (ev.key === "Enter" || ev.key === " ") { ev.preventDefault(); elegirOrigen(e.id); }
        });
        lienzo.appendChild(grupo);
    }
    contenedor.replaceChildren(lienzo);

    const select = $("selectOrigen");
    select.replaceChildren(...estaciones.map((e) => {
        const opcion = crear("option", null, `${e.nombre} (${e.id})`);
        opcion.value = e.id;
        return opcion;
    }));
    select.disabled = false;
    $("btnRecorrer").disabled = false;
    elegirOrigen(estaciones.some((e) => e.id === "SOL") ? "SOL" : estaciones[0].id);

    const selectDijkstraOrigen = $("dijkstraOrigen");
    const selectDijkstraDestino = $("dijkstraDestino");
    if (selectDijkstraOrigen && selectDijkstraDestino) {
        selectDijkstraOrigen.replaceChildren(...estaciones.map((e) => {
            const opcion = crear("option", null, `${e.nombre} (${e.id})`);
            opcion.value = e.id;
            return opcion;
        }));
        selectDijkstraDestino.replaceChildren(...estaciones.map((e) => {
            const opcion = crear("option", null, `${e.nombre} (${e.id})`);
            opcion.value = e.id;
            return opcion;
        }));
        if (estaciones.some((e) => e.id === "SOL")) selectDijkstraOrigen.value = "SOL";
        if (estaciones.some((e) => e.id === "CITADEL")) selectDijkstraDestino.value = "CITADEL";
    }
}

function nodoDe(id) {
    return document.querySelector(`.estacion[data-id="${CSS.escape(id)}"]`);
}

function limpiarRecorrido() {
    estado.animacion++;
    document.querySelectorAll(".estacion").forEach((g) => {
        g.classList.remove("estacion--visitada", "estacion--actual");
        const etiqueta = g.querySelector(".estacion__id, .estacion__paso");
        etiqueta.setAttribute("class", "estacion__id");
        etiqueta.textContent = g.dataset.id;
    });
    $("listaRecorrido").replaceChildren();
    $("recorridoVacio").hidden = false;
}

function elegirOrigen(id) {
    estado.origen = id;
    $("selectOrigen").value = id;
    limpiarRecorrido();
    document.querySelectorAll(".estacion").forEach((g) => g.classList.toggle("estacion--origen", g.dataset.id === id));
}

async function recorrer() {
    const tipo = document.querySelector('input[name="tipoRecorrido"]:checked').value;
    let resultado;
    try {
        resultado = await conBotonOcupado($("btnRecorrer"), "Recorriendo…",
            () => api(`/api/grafo/recorrer?origen=${encodeURIComponent(estado.origen)}&tipo=${tipo}`));
    } catch (e) {
        avisar(e.message);
        return;
    }

    limpiarRecorrido();
    const turno = estado.animacion;
    $("recorridoVacio").hidden = true;
    const idPorNombre = new Map(estado.grafo.estaciones.map((e) => [e.nombre, e.id]));
    const lista = $("listaRecorrido");
    let anterior = null;

    for (let i = 0; i < resultado.ordenExploracion.length; i++) {
        if (turno !== estado.animacion) return; // se eligió otro origen o se relanzó
        const nombre = resultado.ordenExploracion[i];

        const item = crear("li");
        item.append(crear("span", "recorrido__num", i + 1), crear("span", null, nombre));
        lista.appendChild(item);

        const nodo = idPorNombre.has(nombre) ? nodoDe(idPorNombre.get(nombre)) : null;
        if (nodo) {
            if (anterior) anterior.classList.remove("estacion--actual");
            nodo.classList.add("estacion--visitada", "estacion--actual");
            const etiqueta = nodo.querySelector(".estacion__id");
            etiqueta.setAttribute("class", "estacion__paso");
            etiqueta.textContent = i + 1;
            anterior = nodo;
        }
        if (!sinAnimacion) await new Promise((r) => setTimeout(r, 450));
    }
    if (anterior && turno === estado.animacion) anterior.classList.remove("estacion--actual");
}

const TEORIA_RECORRIDO = {
    BFS: "<strong>BFS (anchura).</strong> Visita primero todo lo que está a 1 salto del origen, después a 2, y así. "
        + "Usa una <strong>cola FIFO</strong> y marca cada estación al encolarla. Tiempo <code>O(V + E)</code>, espacio <code>O(V)</code>.",
    DFS: "<strong>DFS (profundidad).</strong> Avanza por un camino hasta no poder seguir y recién ahí retrocede. "
        + "Usa la <strong>pila de llamadas recursivas</strong>. Tiempo <code>O(V + E)</code>, espacio <code>O(V)</code>."
};

function mostrarTeoriaRecorrido() {
    const tipo = document.querySelector('input[name="tipoRecorrido"]:checked').value;
    $("teoriaRecorrido").innerHTML = TEORIA_RECORRIDO[tipo]
        + " Los vecinos se exploran en orden alfabético de id.";
}

/* ============================ Bodega ============================ */

const MINERALES_EJEMPLO = [
    { nombre: "Cristal de Taquiones", peso: 6, valor: 66 },
    { nombre: "Núcleo de Plasma", peso: 5, valor: 50 },
    { nombre: "Aleación de Titanio", peso: 5, valor: 50 },
    { nombre: "Fragmento de Antimateria", peso: 2, valor: 30 },
    { nombre: "Lingote de Iridio", peso: 4, valor: 44 },
    { nombre: "Celdas de Helio-3", peso: 3, valor: 27 }
];

async function cargarMinerales() {
    try {
        estado.minerales = await api("/api/minerales");
        dibujarMinerales(estado.minerales);
    } catch (e) {
        estado.minerales = [];
        $("contadorMinerales").textContent = "sin datos";
        const celda = crear("td", "vacio", `No se pudieron leer los minerales: ${e.message}`);
        celda.colSpan = 5;
        const fila = crear("tr");
        fila.appendChild(celda);
        $("tablaMinerales").replaceChildren(fila);
    }
}

function dibujarMinerales(minerales, animar = false) {
    const cuerpo = $("tablaMinerales");
    $("contadorMinerales").textContent = `${minerales.length} ${minerales.length === 1 ? "lote" : "lotes"}`;

    if (minerales.length === 0) {
        const celda = crear("td", "vacio", "La bodega está vacía. Agregá un mineral con el formulario o ");
        celda.colSpan = 5;
        const ejemplo = crear("button", "boton boton--enlace", "cargá los minerales de ejemplo");
        ejemplo.type = "button";
        ejemplo.addEventListener("click", () => guardarMinerales(MINERALES_EJEMPLO, ejemplo, "Guardando…"));
        celda.append(ejemplo, ".");
        const fila = crear("tr");
        fila.appendChild(celda);
        cuerpo.replaceChildren(fila);
        return;
    }

    cuerpo.replaceChildren(...minerales.map((m) => {
        const fila = crear("tr", animar ? "fila-nueva" : null);
        const borrar = crear("button", "boton boton--icono", "✕");
        borrar.type = "button";
        borrar.title = `Eliminar ${m.nombre}`;
        borrar.setAttribute("aria-label", `Eliminar ${m.nombre}`);
        borrar.addEventListener("click", () => eliminarMineral(m, borrar));
        const acciones = crear("td");
        acciones.appendChild(borrar);
        // textContent en todas las celdas: los nombres vienen de la base y no se interpretan como HTML
        fila.append(crear("td", null, m.nombre), crear("td", "num", numero(m.peso)),
            crear("td", "num", numero(m.valor)), crear("td", "num", numero(m.ratio)), acciones);
        return fila;
    }));
}

async function guardarMinerales(lista, boton, textoOcupado) {
    try {
        await conBotonOcupado(boton, textoOcupado, () => enviarJson("/api/minerales", lista));
        await cargarMinerales();
        return true;
    } catch (e) {
        avisar(e.message);
        return false;
    }
}

async function agregarMineral(evento) {
    evento.preventDefault();
    const mineral = {
        nombre: $("nuevoNombre").value.trim(),
        peso: parseFloat($("nuevoPeso").value),
        valor: parseFloat($("nuevoValor").value)
    };
    if (await guardarMinerales([mineral], $("btnAgregar"), "Guardando…")) {
        $("formMineral").reset();
        $("nuevoNombre").focus();
        avisar(`${mineral.nombre} guardado en la base.`, "ok");
    }
}

async function eliminarMineral(mineral, boton) {
    if (!confirm(`¿Eliminar "${mineral.nombre}" de la base?`)) return;
    boton.disabled = true;
    try {
        await api(`/api/minerales/${encodeURIComponent(mineral.id)}`, { method: "DELETE" });
    } catch (e) {
        if (e.codigo !== 404) avisar(e.message); // 404: ya lo había borrado otra persona
    }
    await cargarMinerales();
}

async function ordenarMinerales() {
    if (estado.minerales.length === 0) {
        avisar("La bodega está vacía: agregá minerales antes de ordenar.");
        return;
    }
    const algoritmo = document.querySelector('input[name="algoritmo"]:checked').value;
    const direccion = document.querySelector('input[name="direccion"]:checked').value;
    const criterio = $("selectCriterio").value;
    const inicio = performance.now();
    try {
        // Sin body: el servidor ordena los minerales persistidos en Neo4j
        const r = await conBotonOcupado($("btnOrdenar"), "Ordenando…",
            () => api(`/api/minerales/ordenar?algoritmo=${algoritmo}&criterio=${criterio}&direccion=${direccion}`, { method: "POST" }));
        estado.minerales = r.resultado;
        dibujarMinerales(estado.minerales, true);
        const linea = $("resultadoOrden");
        linea.replaceChildren(crear("strong", null, r.algoritmoUtilizado),
            ` · por ${r.criterio}, ${direccion === "desc" ? "de mayor a menor" : "de menor a mayor"} · respuesta en ${Math.round(performance.now() - inicio)} ms`);
    } catch (e) {
        avisar(e.message);
    }
}

const TEORIA_ORDEN = {
    quicksort: "<strong>QuickSort.</strong> Pivote por mediana de tres y partición de 3 vías (menores, iguales, mayores). "
        + "Promedio <code>O(N log N)</code>; peor caso <code>O(N²)</code>. <strong>No es estable</strong>: dos minerales con la misma clave pueden salir en cualquier orden.",
    mergesort: "<strong>MergeSort.</strong> Divide a la mitad, ordena cada parte y las mezcla. "
        + "<code>O(N log N)</code> garantizado y <code>O(N)</code> de espacio extra. <strong>Es estable</strong>: respeta el orden original entre claves iguales."
};

function mostrarTeoriaOrden() {
    $("teoriaOrden").innerHTML = TEORIA_ORDEN[document.querySelector('input[name="algoritmo"]:checked').value];
}

/* ============================ Carga rápida (Greedy) ============================ */

const claveMineral = (m) => `${m.nombre}|${m.peso}|${m.valor}`;

async function cargarBodega(evento) {
    evento.preventDefault();
    if (estado.minerales.length === 0) {
        avisar("No hay minerales guardados. Agregalos en la pestaña Bodega.");
        return;
    }
    const capacidad = parseFloat($("capacidadBodega").value);
    const disponibles = estado.minerales.map(({ nombre, peso, valor }) => ({ nombre, peso, valor }));
    let r;
    try {
        r = await conBotonOcupado($("btnCargar"), "Cargando…",
            () => enviarJson("/api/bodega/cargar-greedy", { capacidadBodega: capacidad, itemsDisponibles: disponibles }));
    } catch (e) {
        avisar(e.message);
        return;
    }

    $("cargaVacio").hidden = true;
    $("cargaResultado").hidden = false;
    $("cargaValor").textContent = numero(r.valorTotalObtenido);
    $("cargaPeso").textContent = numero(r.pesoOcupado);
    $("cargaCantidad").textContent = r.mineralesCargados.length;

    const porcentaje = r.capacidadBodega > 0 ? Math.min(100, (r.pesoOcupado / r.capacidadBodega) * 100) : 0;
    $("cargaBarraRelleno").style.width = `${porcentaje}%`;
    const textoBarra = `${numero(r.pesoOcupado)} de ${numero(r.capacidadBodega)} t ocupadas (${Math.round(porcentaje)} %)`;
    $("cargaBarraTexto").textContent = textoBarra;
    $("cargaBarra").setAttribute("aria-label", `Ocupación de la bodega: ${textoBarra}`);

    const itemDe = (m) => {
        const li = crear("li");
        li.append(crear("span", null, m.nombre),
            crear("span", null, `${numero(m.peso)} t · ${numero(m.valor)} CG · ratio ${numero(m.valor / m.peso)}`));
        return li;
    };
    $("cargaLista").replaceChildren(...r.mineralesCargados.map(itemDe));
    if (r.mineralesCargados.length === 0) {
        $("cargaLista").replaceChildren(crear("li", null, "Ningún mineral entra en esa capacidad."));
    }

    // Lo que quedó afuera = disponibles menos cargados (como multiconjunto, por si hay lotes repetidos)
    const cargados = new Map();
    r.mineralesCargados.forEach((m) => cargados.set(claveMineral(m), (cargados.get(claveMineral(m)) || 0) + 1));
    const afuera = disponibles.filter((m) => {
        const quedan = cargados.get(claveMineral(m)) || 0;
        if (quedan > 0) { cargados.set(claveMineral(m), quedan - 1); return false; }
        return true;
    });
    $("cargaAfueraTitulo").hidden = afuera.length === 0;
    $("cargaAfuera").replaceChildren(...afuera.map(itemDe));
}

/* ============================ Hito 5: Salto Hiperespacial y MST ============================ */

async function ejecutarDijkstra() {
    const origen = $("dijkstraOrigen").value;
    const destino = $("dijkstraDestino").value;
    if (!origen || !destino) {
        avisar("Seleccioná las estaciones de origen y destino.");
        return;
    }
    let r;
    try {
        r = await conBotonOcupado($("btnDijkstra"), "Calculando trayectoria…",
            () => api(`/api/navegacion/dijkstra?origen=${encodeURIComponent(origen)}&destino=${encodeURIComponent(destino)}`));
    } catch (e) {
        avisar(e.message);
        return;
    }

    $("dijkstraResultado").hidden = false;
    $("dijkstraConsumo").textContent = numero(r.consumoTotal);
    $("dijkstraSaltos").textContent = (r.caminoReconstruido && r.caminoReconstruido.length > 0)
        ? r.caminoReconstruido.length - 1
        : 0;

    const lista = $("dijkstraCamino");
    lista.replaceChildren(...r.caminoReconstruido.map((nombre, i) => {
        const id = r.caminoIds && r.caminoIds[i] ? ` (${r.caminoIds[i]})` : "";
        const li = crear("li");
        li.append(crear("span", "recorrido__num", i + 1), crear("span", null, `${nombre}${id}`));
        return li;
    }));
}

async function ejecutarMst() {
    const radio = document.querySelector('input[name="tipoMst"]:checked');
    const metodo = radio ? radio.value : "kruskal";
    let r;
    try {
        r = await conBotonOcupado($("btnMst"), "Generando red troncal…",
            () => api(`/api/red/mst?metodo=${encodeURIComponent(metodo)}`));
    } catch (e) {
        avisar(e.message);
        return;
    }

    $("mstResultado").hidden = false;
    $("mstCosto").textContent = numero(r.costoTotalMST);
    $("mstCantidadAristas").textContent = r.aristasSeleccionadas ? r.aristasSeleccionadas.length : 0;

    const lista = $("mstListaAristas");
    lista.replaceChildren(...(r.aristasSeleccionadas || []).map((arista) => {
        const li = crear("li");
        li.append(
            crear("span", null, `${arista.origen} ⇄ ${arista.destino}`),
            crear("span", null, `${numero(arista.costo)} CA`)
        );
        return li;
    }));
}

/* ============================ Inicio ============================ */

function cargarTodo() {
    consultarEstado();
    cargarMapa();
    cargarMinerales();
}

document.addEventListener("DOMContentLoaded", () => {
    iniciarPestanias();

    $("estadoBase").addEventListener("click", cargarTodo);
    $("selectOrigen").addEventListener("change", (e) => elegirOrigen(e.target.value));
    $("btnRecorrer").addEventListener("click", recorrer);
    document.querySelectorAll('input[name="tipoRecorrido"]').forEach((r) => r.addEventListener("change", () => {
        mostrarTeoriaRecorrido();
        limpiarRecorrido();
    }));

    $("formMineral").addEventListener("submit", agregarMineral);
    $("btnOrdenar").addEventListener("click", ordenarMinerales);
    document.querySelectorAll('input[name="algoritmo"]').forEach((r) => r.addEventListener("change", mostrarTeoriaOrden));

    $("formCarga").addEventListener("submit", cargarBodega);

    $("btnDijkstra").addEventListener("click", ejecutarDijkstra);
    $("btnMst").addEventListener("click", ejecutarMst);

    mostrarTeoriaRecorrido();
    mostrarTeoriaOrden();
    cargarTodo();
});
