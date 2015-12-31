TARGETS=

all : ${TARGETS}

.PRECIOUS: %.png %.pdf %.map

%.eog : %.png
	eog $*.png

%.qiv : %.png
	qiv -mf $*.png

%.png : %.dot
	dot -Tpng -o $*.png $*.dot

%.pdf : %.dot
	dot -Tpdf -o $*.pdf $*.dot

%.svgz : %.dot
	dot -Tsvgz -o $*.svgz $*.dot

%.map : %.dot
	dot -Tcmapx -o $*.map $*.dot

clean:
	rm -f ${TARGETS}
