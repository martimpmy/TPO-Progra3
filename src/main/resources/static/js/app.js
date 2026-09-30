// Odisea Galáctica - Consola de Navegación e Inventario
document.addEventListener("DOMContentLoaded", () => {
    inicializarTelemetriaGrafo();
    inicializarInventario();
});

// Lista reactiva de minerales en la bahía de carga
let mineralesEnBodega = [
    { nombre: "Cristal de Taquiones", peso: 6, valor: 66, ratio: 11.0 },
    { nombre: "Núcleo de Plasma", peso: 5, valor: 50, ratio: 10.0 },
    { nombre: "Aleación de Titanio", peso: 5, valor: 50, ratio: 10.0 },
    { nombre: "Fragmento de Antimateria", peso: 2, valor: 30, ratio: 15.0 },
    { nombre: "Lingote de Iridio", peso: 4, valor: 44, ratio: 11.0 },
    { nombre: "Celdas de Helio-3", peso: 3, valor: 27, ratio: 9.0 }
];

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
    renderizarTablaMinerales(mineralesEnBodega);

    const btnOrdenar = document.getElementById("btnEjecutarOrden");
    const btnAgregar = document.getElementById("btnAgregarMineral");

    btnOrdenar.addEventListener("click", ejecutarOrdenamiento);
    btnAgregar.addEventListener("click", agregarMineral);

    // Actualizar recuadro teórico cuando cambie el algoritmo
    document.getElementById("selectAlgoritmo").addEventListener("change", actualizarTeoria);
    actualizarTeoria();
}

function renderizarTablaMinerales(minerales, animar = false) {
    const tbody = document.getElementById("mineralesTableBody");
    tbody.innerHTML = "";

    minerales.forEach((m) => {
        const tr = document.createElement("tr");
        if (animar) tr.classList.add("animate-sort");

        const ratio = m.ratio !== undefined ? m.ratio : (m.peso > 0 ? (m.valor / m.peso).toFixed(1) : 0);

        tr.innerHTML = `
            <td><strong>${m.nombre}</strong></td>
            <td>${m.peso} t</td>
            <td>${m.valor} CG</td>
            <td><span class="ratio-pill">${ratio} CG/t</span></td>
        `;
        tbody.appendChild(tr);
    });

    document.getElementById("contadorMinerales").textContent = `${minerales.length} lotes`;
}

async function ejecutarOrdenamiento() {
    const algoritmo = document.getElementById("selectAlgoritmo").value;
    const criterio = document.getElementById("selectCriterio").value;
    const direccion = document.getElementById("selectDireccion").value;
    const btn = document.getElementById("btnEjecutarOrden");

    btn.disabled = true;
    btn.innerHTML = "⚡ PROCESANDO DIVIDE Y VENCERÁS...";

    const tInicio = performance.now();

    try {
        const url = `/api/minerales/ordenar?algoritmo=${encodeURIComponent(algoritmo)}&criterio=${encodeURIComponent(criterio)}&direccion=${encodeURIComponent(direccion)}`;

        // Enviamos la lista actual en memoria
        const response = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(mineralesEnBodega)
        });

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

function agregarMineral(e) {
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

    const ratio = Math.round((valor / peso) * 100.0) / 100.0;
    mineralesEnBodega.push({ nombre, peso, valor, ratio });
    renderizarTablaMinerales(mineralesEnBodega);

    inputNombre.value = "";
    inputPeso.value = "";
    inputValor.value = "";
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
