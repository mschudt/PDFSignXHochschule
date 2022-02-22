#!/bin/sh
mvn clean compile assembly:single
cp -f target/PDFSignXHochschule.jar .
