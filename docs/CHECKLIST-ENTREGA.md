# Checklist de entrega — Hito 12

Validación de los requisitos de admisibilidad antes de crear el tag `entrega-final` y entregar en Teams.
Combina la lista "Antes de entregar" del README original de la cátedra con las reglas del informe
(secciones 3.2 y 4.3) cuyo incumplimiento penaliza o anula la entrega.

## Requisitos

| # | Requisito | Estado | Evidencia |
|---|---|---|---|
| 1 | `./mvnw test` pasa | ✅ | Toda la suite en verde, sin necesitar Neo4j (ver `docs/AUDITORIA-TESTS.md`) |
| 2 | Ninguna contraseña en el código ni en `application.properties` | ✅ | Credenciales solo por variables de entorno o `.env`; `.env` está en `.gitignore` y no se versionó nunca |
| 3 | Ninguna contraseña en el **historial de git** (informe, sección 4.3) | ❌ **Pendiente** | `spring.neo4j.authentication.password=***ELIMINADO***` en los commits `9a75614`, `b3ee7a7` y `c3c5850`. Ver "Limpiar el historial" |
| 4 | Cada algoritmo implementado tiene al menos un test | ✅ | Un `…Test` por clase de `algorithm/`; `Grafo` se prueba en `RecorridosTest` |
| 5 | `docs/ENDPOINTS.md` documenta entrada y salida de cada endpoint | ✅ | Los 14 endpoints, con parámetros, ejemplo real, errores, complejidad y estructura usada |
| 6 | El README explica el dominio y qué representa el costo | ✅ | Sección "Dominio galáctico": costo de una ruta = Celdas de Antimateria (CA) |

Reglas de diseño del informe, también verificadas:

| Regla | Estado | Cómo se verificó |
|---|---|---|
| Sin `Collections.sort`, `Arrays.sort`, `List.sort` ni librerías de grafos | ✅ | Búsqueda en `src/main`; el `pom.xml` solo tiene dependencias de Spring Boot y JaCoCo |
| Controllers sin lógica algorítmica | ✅ | Ningún `for`/`while` en `controller/` |
| Algoritmos sin acceso a la base ni a Spring | ✅ | Ninguna clase de `algorithm/` importa Spring ni repositorios |
| El grafo se lee una sola vez por pedido | ✅ | `GrafoServiceTest` verifica una única llamada a `leerGrafo()` |
| Errores sin stack trace | ✅ | `server.error.include-stacktrace=never` y `ManejadorDeErroresTest` |

## Limpiar el historial (requisito 3)

**Coordinarlo con el grupo antes**: reescribe todos los commits y cada integrante tiene que volver a clonar.
Si `***ELIMINADO***` se usó en algún otro lado, cambiarla.

```bash
# 1. Herramienta
pip install git-filter-repo

# 2. Trabajar sobre un clon nuevo (filter-repo lo exige)
git clone https://github.com/martimpmy/TPO-Progra3.git tpo-limpio
cd tpo-limpio

# 3. Reemplazar la contraseña en todo el historial
echo '***ELIMINADO***==>***ELIMINADO***' > ../reemplazos.txt
git filter-repo --replace-text ../reemplazos.txt
git log --all -p | grep -c ***ELIMINADO***     # tiene que dar 0

# 4. Publicar (sobrescribe GitHub)
git remote add origin https://github.com/martimpmy/TPO-Progra3.git
git push --force --all
git push --force --tags
```

Después, cada integrante borra su copia local y vuelve a clonar.

## Tag y entrega

Recién cuando los 6 requisitos estén en verde (y **después** de limpiar el historial, porque eso cambia los commits):

```bash
./mvnw test                                    # última verificación
git tag -a entrega-final -m "Entrega final TPO Programación III"
git push origin entrega-final
```

Entregar en Teams el enlace al repositorio y al tag `entrega-final`.
