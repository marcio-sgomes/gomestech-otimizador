#!/usr/bin/env python3
"""Confere a cobertura de testes do pacote de lógica (logica/) a partir do relatório JaCoCo.
Falha (código 1) se alguma linha da lógica não foi testada."""
import sys
import xml.etree.ElementTree as ET

relatorio = sys.argv[1]
pacote_alvo = "br/com/gomestech/otimizador/logica"
raiz = ET.parse(relatorio).getroot()

pacote = next((p for p in raiz.iter("package") if p.get("name") == pacote_alvo), None)
if pacote is None:
    print(f"ERRO: pacote {pacote_alvo} não encontrado no relatório")
    sys.exit(1)

def contador(elem, tipo):
    for c in elem.findall("counter"):
        if c.get("type") == tipo:
            return int(c.get("missed")), int(c.get("covered"))
    return 0, 0

print("Cobertura do pacote logica/:")
falhou = False
for tipo in ("LINE", "BRANCH", "INSTRUCTION", "METHOD"):
    m, c = contador(pacote, tipo)
    total = m + c
    pct = 100.0 * c / total if total else 100.0
    print(f"  {tipo:12s} {pct:6.2f}%  ({c}/{total})")

for arquivo in pacote.findall("sourcefile"):
    m, c = contador(arquivo, "LINE")
    if m:
        falhou = True
        linhas = [l.get("nr") for l in arquivo.findall("line") if int(l.get("mi")) > 0]
        print(f"  FALTA TESTAR {arquivo.get('name')}: linhas {', '.join(linhas)}")

for arquivo in pacote.findall("sourcefile"):
    linhas = [l.get("nr") for l in arquivo.findall("line") if int(l.get("mb", "0")) > 0]
    if linhas:
        falhou = True
        print(f"  FALTA TESTAR CAMINHO (if/else) {arquivo.get('name')}: linhas {', '.join(linhas)}")

ml, _ = contador(pacote, "LINE")
mb, _ = contador(pacote, "BRANCH")
if ml or mb or falhou:
    print("ERRO: a cobertura da lógica precisa ser 100% (linhas e caminhos).")
    sys.exit(1)
print("OK: 100% das linhas e 100% dos caminhos da lógica testados.")
