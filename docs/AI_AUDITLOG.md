==================================================
EXECUTION LOG / CRASH RECOVERY — MANDATORY
==================================================

Maintain a current execution log throughout the audit:

docs/reports/[NO. OF TASK ++]-[SUBWORKING NO.]-[TASK]-EXECUTION_LOG.md

This is mandatory even though the audit is READ-ONLY.

The execution log must be created or updated continuously after
meaningful audit milestones, NOT only at the end.

The log must preserve the latest verified state so that work can be
resumed safely after an agent crash, terminal failure, streaming
failure, IDE restart, or interrupted session.

Record only verified facts. Never invent findings or validation results.

The execution log must contain:

- current status
- audit date/time
- current Git branch and HEAD
- audit scope
- completed audit sections
- actual findings
- evidence / file references
- GREEN / YELLOW / ORANGE / RED / GRAY classification
- Terraform checks actually executed and their results
- Git status
- files changed, if any
- explicit confirmation when no files were changed
- open questions
- risks
- recommended next actions
- current resume point

After each major section, update the execution log before continuing.

At the end, finalize the log with the complete audit summary.

IMPORTANT:
The execution log itself is part of the audit workflow and must be
kept accurate even if the audit remains completely read-only.

==================================================

## SUBAGENT-REPORT-PFLICHT (verbindlich)

Jeder beauftragte Subagent (Research/Explore/Review — read-only oder schreibend)
liefert sein Ergebnis als strukturierten Report nach den 16 Pflichtpunkten des
Execution-Log-Templates oben. Die Agenten-Beschreibung (Prompt) MUSS diese
Report-Pflicht ausdrücklich enthalten. Der Orchestrator trägt die Subagent-
Reports eigenständig nach unter:

    docs/reports/<TASK-ID>-<SUBWORKING>-<AUFGABE>-EXECUTION_LOG.md

und indiziert sie unten im AUDIT LOG INDEX. So sind Research-Ergebnisse
crash-sicher, revisionsfähig und für Folge-Checkpoints wiederverwendbar —
der Chatverlauf ist keine Recovery-Quelle.

==================================================

## AUDIT LOG INDEX
# please use AI_AUDITINDEX.md for indexind reportfiles
# example
Current Branch: main
Latest HEAD: siehe `git log -1` ([Name] vor diesem Doku-Commit: [7 digits])

The following tasks have been completed with reports in docs/reports/:
- B8.6 FINAL-[Stepnames]-[fe anitherpart of]-[count like 01] → like -> docs/reports/BACKUP-08.6-FINAL-REMOTE-STATE-RECOVERY-AUDIT.md

