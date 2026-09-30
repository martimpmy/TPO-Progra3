// Odisea Galáctica - Consola de Navegación e Inventario
document.addEventListener("DOMContentLoaded", () => {
    inicializarTelemetriaGrafo();
    inicializarInventario();
});

// Minerales mostrados en la tabla (se leen de Neo4j vía /api/minerales)
let mineralesEnBodega = [];

/**
 * Consulta el estado del Hito 1 (Neo4j AuraDB y resumen del grafo)
 */
async function inicializarTelemetriaGrafo() {
    const badge = document.getElementById("grafoStatusBadge");
    const statusText = document.getElementById("grafoStatusText");

    try {
        const response = await fetch("/api/grafo/resumen");
        if (!response.ok) throw new Error("Fallo de conexión");
        const data = await response.json();

        badge.className = "status-badge";
        statusText.textContent = `AuraDB En Línea: ${data.vertices} Estaciones | ${data.aristas} Rutas`;
    } catch (error) {
        badge.className = "status-badge loading";
        statusText.textContent = "AuraDB Desconectado / Sin Credenciales";
    }
}

/**
 * Inicializa y enlaza los eventos de la consola de minerales (Hito 2)
 */
function inicializarInventario() {
    cargarMineralesPersistidos();

    const btnOrdenar = document.getElementById("btnEjecutarOrden");
    const formAgregar = document.getElementById("formAgregarMineral");

    btnOrdenar.addEventListener("click", ejecutarOrdenamiento);
    // submit (botón o Enter); el navegador valida required/min antes de disparar el evento
    formAgregar.addEventListener("submit", agregarMineral);

    // Actualizar recuadro teórico cuando cambie el algoritmo
    document.getElementById("selectAlgoritmo").addEventListener("change", actualizarTeoria);
    actualizarTeoria();
}

async function cargarMineralesPersistidos() {
    try {
        const response = await fetch("/api/minerales");
        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.mensaje);
        }
        mineralesEnBodega = await response.json();
        renderizarTablaMinerales(mineralesEnBodega);
    } catch (err) {
        mineralesEnBodega = [];
        renderizarTablaMinerales(mineralesEnBodega);
        alert(`No se pudieron leer los minerales: ${err.message}`);
    }
}

function celda(texto) {
    const td = document.createElement("td");
    td.textContent = texto;
    return td;
}

function renderizarTablaMinerales(minerales, animar = false) {
    const tbody = document.getElementById("mineralesTableBody");
    tbody.innerHTML = "";

    if (minerales.length === 0) {
        const tr = document.createElement("tr");
        const td = celda("La bodega está vacía. Agregá minerales con el formulario o ");
        td.colSpan = 5;
        const btnEjemplo = document.createElement("button");
        btnEjemplo.type = "button";
        btnEjemplo.className = "btn-secondary";
        btnEjemplo.textContent = "cargá el ejemplo del PDF";
        btnEjemplo.addEventListener("click", cargarEjemplo);
        td.appendChild(btnEjemplo);
        tr.appendChild(td);
        tbody.appendChild(tr);
    }

    minerales.forEach((m) => {
        const tr = document.createElement("tr");
        if (animar) tr.classList.add("animate-sort");

        // textContent (no innerHTML): los nombres vienen de la base y no deben interpretarse como HTML
        const tdNombre = document.createElement("td");
        const strong = document.createElement("strong");
        strong.textContent = m.nombre;
        tdNombre.appendChild(strong);

        const tdRatio = document.createElement("td");
        const pill = document.createElement("span");
        pill.className = "ratio-pill";
        pill.textContent = `${m.ratio} CG/t`;
        tdRatio.appendChild(pill);

        const tdAcciones = document.createElement("td");
        const btnBorrar = document.createElement("button");
        btnBorrar.className = "btn-secondary";
        btnBorrar.type = "button";
        btnBorrar.title = "Eliminar de la base";
        btnBorrar.textContent = "✕";
        btnBorrar.addEventListener("click", () => eliminarMineral(m.id, m.nombre));
        tdAcciones.appendChild(btnBorrar);

        tr.append(tdNombre, celda(`${m.peso} t`), celda(`${m.valor} CG`), tdRatio, tdAcciones);
        tbody.appendChild(tr);
    });

    document.getElementById("contadorMinerales").textContent = `${minerales.length} lotes`;
}

// Contraejemplo Greedy vs PD del informe (sección 5.6) + tres lotes extra
const MINERALES_EJEMPLO = [
    { nombre: "Cristal de Taquiones", peso: 6, valor: 66 },
    { nombre: "Núcleo de Plasma", peso: 5, valor: 50 },
    { nombre: "Aleación de Titanio", peso: 5, valor: 50 },
    { nombre: "Fragmento de Antimateria", peso: 2, valor: 30 },
    { nombre: "Lingote de Iridio", peso: 4, valor: 44 },
    { nombre: "Celdas de Helio-3", peso: 3, valor: 27 }
];

async function cargarEjemplo(e) {
    e.target.disabled = true;
    e.target.textContent = "Guardando en AuraDB...";
    await guardarMinerales(MINERALES_EJEMPLO);
}

/** POST /api/minerales y recarga la tabla. Devuelve true si se guardó. */
async function guardarMinerales(lista) {
    try {
        const response = await fetch("/api/minerales", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(lista)
        });
        if (!response.ok) {
            const err = await response.json();
            alert(`Error (${err.codigo}): ${err.mensaje}`);
            await cargarMineralesPersistidos();
            return false;
        }
        await cargarMineralesPersistidos();
        return true;
    } catch (err) {
        alert("Error de conexión al guardar en la base.");
        await cargarMineralesPersistidos();
        return false;
    }
}

async function ejecutarOrdenamiento() {
    const algoritmo = document.getElementById("selectAlgoritmo").value;
    const criterio = document.getElementById("selectCriterio").value;
    const direccion = document.getElementById("selectDireccion").value;
    const btn = document.getElementById("btnEjecutarOrden");

    if (mineralesEnBodega.length === 0) {
        alert("La bodega está vacía: agregá minerales antes de ordenar.");
        return;
    }

    btn.disabled = true;
    btn.innerHTML = "⚡ PROCESANDO DIVIDE Y VENCERÁS...";

    const tInicio = performance.now();

    try {
        const url = `/api/minerales/ordenar?algoritmo=${encodeURIComponent(algoritmo)}&criterio=${encodeURIComponent(criterio)}&direccion=${encodeURIComponent(direccion)}`;

        // Sin body: el servidor ordena los minerales persistidos en Neo4j
        const response = await fetch(url, { method: "POST" });

        if (!response.ok) {
            const err = await response.json();
            alert(`Error (${err.codigo}): ${err.mensaje}`);
            return;
        }

        const data = await response.json();
        const tFin = performance.now();
        const ms = (tFin - tInicio).toFixed(1);

        // Actualizamos estado local y renderizamos
        mineralesEnBodega = data.resultado;
        renderizarTablaMinerales(mineralesEnBodega, true);

        // Actualizar métricas
        document.getElementById("metricaAlgoritmo").textContent = data.algoritmoUtilizado;
        document.getElementById("metricaTiempo").textContent = `${ms} ms`;
        document.getElementById("metricaCriterio").textContent = `${data.criterio.toUpperCase()} (${direccion.toUpperCase()})`;

    } catch (err) {
        console.error("Error al ordenar:", err);
        alert("Error de conexión al ejecutar el ordenamiento.");
    } finally {
        btn.disabled = false;
        btn.innerHTML = "⚡ EJECUTAR ORDENAMIENTO ALGORÍTMICO";
    }
}

async function agregarMineral(e) {
    e.preventDefault();
    const inputNombre = document.getElementById("nuevoNombre");
    const inputPeso = document.getElementById("nuevoPeso");
    const inputValor = document.getElementById("nuevoValor");

    const nombre = inputNombre.value.trim();
    const peso = parseFloat(inputPeso.value);
    const valor = parseFloat(inputValor.value);

    if (!nombre) {
        alert("Ingrese un nombre de mineral.");
        return;
    }
    if (isNaN(peso) || peso <= 0) {
        alert("El peso debe ser mayor a 0.");
        return;
    }
    if (isNaN(valor) || valor < 0) {
        alert("El valor no puede ser negativo.");
        return;
    }

    const btn = document.getElementById("btnAgregarMineral");
    btn.disabled = true;
    btn.textContent = "Guardando...";
    const ok = await guardarMinerales([{ nombre, peso, valor }]);
    btn.disabled = false;
    btn.textContent = "+ Agregar";

    if (ok) {
        inputNombre.value = "";
        inputPeso.value = "";
        inputValor.value = "";
        inputNombre.focus();
    }
}

async function eliminarMineral(id, nombre) {
    if (!confirm(`¿Eliminar "${nombre}" de la base?`)) return;
    try {
        const response = await fetch(`/api/minerales/${encodeURIComponent(id)}`, { method: "DELETE" });
        if (!response.ok && response.status !== 404) {
            const err = await response.json();
            alert(`Error (${err.codigo}): ${err.mensaje}`);
            return;
        }
        await cargarMineralesPersistidos();
    } catch (err) {
        alert("Error de conexión al eliminar el mineral.");
    }
}

function actualizarTeoria() {
    const algo = document.getElementById("selectAlgoritmo").value;
    const box = document.getElementById("teoriaAlgoritmoBox");

    if (algo === "quicksort") {
        box.innerHTML = `
            <strong>QuickSort (Partición de 3 Vías de Dijkstra):</strong><br>
            • <em>Estrategia del Pivote:</em> Mediana de Tres (bajo, medio, alto) + partición de 3 vías (&lt;, =, &gt;).<br>
            • <em>Claves Repetidas:</em> Agrupa elementos con igual clave en <strong>O(N)</strong> en una sola pasada.<br>
            • <em>Optimización de Pila:</em> Eliminación de llamada de cola acotando recursión a <strong>O(log N)</strong>.<br>
            • <em>Recurrencia:</em> T(N) = 2T(N/2) + O(N) ➔ <strong>O(N log N)</strong> promedio por Teorema Maestro.
        `;
    } else {
        box.innerHTML = `
            <strong>MergeSort (Divide y Vencerás Estable):</strong><br>
            • <em>Estrategia de Mezcla:</em> División conceptual a la mitad y fusión lineal respetando estabilidad (&le;).<br>
            • <em>Recurrencia:</em> T(N) = 2T(N/2) + O(N) ➔ <strong>O(N log N)</strong> garantizado en peor y mejor caso.<br>
            • <em>Espacio Auxiliar:</em> O(N) para arreglos de mezcla temporal.
        `;
    }
}
