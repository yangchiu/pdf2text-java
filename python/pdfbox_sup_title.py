from subprocess import Popen, PIPE, STDOUT
import sys

if len(sys.argv) < 2:
    print('usage: python pdfbox_sup_title.py path-to-pdf-file')
else:
    p = Popen(['java', '-jar', 'pdfbox_sup_title.jar', sys.argv[1]],
        stdout=PIPE,
        stderr=STDOUT,
        universal_newlines=True)

for line in p.stdout:
    print(line)