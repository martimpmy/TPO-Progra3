# Checklist de entrega — Hito 12

Validación de los requisitos de admisibilidad antes de crear el tag `entrega-final` y entregar en Teams.
Combina la lista "Antes de entregar" del README original de la cátedra con las reglas del informe
(secciones 3.2 y 4.3) cuyo incumplimiento penaliza o anula la entrega.

## Requisitos

| # | Requisito | Estado | Evidencia |
|---|---|---|---|
| 1 | `./mvnw test` pasa | ✅ | Toda la suite en verde, sin necesitar Neo4j (ver `docs/AUDITORIA-TESTS.md`) |
| 2 | Ninguna contraseña en el código ni en `application.properties` | ✅ | Credenciales solo por variables de entorno o `.env`; `.env` está en `.gitignore` y no se versionó nunca |
| 3 | Ninguna contraseña en el **historial de git** (informe, sección 4.3) | ✅ | La contraseña de Neo4j local que figuraba en `application.properties` en tres commits viejos se eliminó de todo el historial con `git filter-repo` (ver "Limpieza del historial") |
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

## Limpieza del historial (requisito 3)

Tres commits del inicio del proyecto tenían en `application.properties` la contraseña de una base Neo4j local.
Se reemplazó en **todo el historial** con [`git filter-repo`](https://github.com/newren/git-filter-repo)
(`--replace-text`), se verificó que ya no aparece en ningún commit y se publicó con `git push --force`.
Por eso cada integrante tuvo que volver a clonar el repositorio. Si esa contraseña se usaba en algún otro lado,
conviene cambiarla.

## Tag y entrega

Con los 6 requisitos en verde (el tag se crea **después** de limpiar el historial, porque la limpieza cambia los commits):

```bash
./mvnw test                                    # última verificación
git tag -a entrega-final -m "Entrega final TPO Programación III"
git push origin entrega-final
```

Entregar en Teams el enlace al repositorio y al tag `entrega-final`.
