rm *.cert
rm *.jks
rm *.truststore

keytool -genkey -alias server -keyalg RSA -keysize 2048 -keystore keystore.jks -storetype pkcs12 -storepass changeit << EOF
Rafael and Manuella
Thinkpad
FCT
Lisbon
Caparica
PT
yes
EOF

keytool -export -alias server -keystore keystore.jks -file server.cert << EOF
changeit
EOF

keytool -import -file server.cert  -alias server -keystore server.truststore << EOF
changeit
changeit
yes
EOF

