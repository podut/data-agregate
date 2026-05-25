#!/bin/bash
# ─── Script de inițializare Let's Encrypt pentru pixcode.go.ro ────────────────
# Rulează O SINGURĂ DATĂ pentru a obține certificatul SSL.
# Cerință: porturile 80 și 443 să fie deschise în router și să pointeze la acest PC.
#
# Utilizare: bash init-ssl.sh

set -e

DOMAIN="pixcode.go.ro"
EMAIL="podutpetru@gmail.com"

echo "=== PASUL 1: Descărcare parametri SSL recomandați de Let's Encrypt ==="
docker compose run --rm certbot \
  sh -c "curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf \
         -o /etc/letsencrypt/options-ssl-nginx.conf && \
         curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot/certbot/ssl-dhparams.pem \
         -o /etc/letsencrypt/ssl-dhparams.pem"

echo ""
echo "=== PASUL 2: Pornire Nginx cu configurație HTTP-only ==="
# Folosim app-http-only.conf temporar
cp nginx/conf.d/app.conf nginx/conf.d/app.conf.bak
cp nginx/conf.d/app-http-only.conf nginx/conf.d/app.conf

docker compose up -d nginx app db redis
echo "Așteptăm 5 secunde pentru pornirea serviciilor..."
sleep 5

echo ""
echo "=== PASUL 3: Obținere certificat Let's Encrypt pentru $DOMAIN ==="
docker compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  -d "$DOMAIN"

echo ""
echo "=== PASUL 4: Restaurare configurație Nginx cu HTTPS ==="
cp nginx/conf.d/app.conf.bak nginx/conf.d/app.conf
rm -f nginx/conf.d/app.conf.bak

echo ""
echo "=== PASUL 5: Restart Nginx cu HTTPS activ ==="
docker compose restart nginx

echo ""
echo "======================================================================"
echo " SUCCES! Certificatul SSL a fost obtinut."
echo " Serverul este disponibil la: https://$DOMAIN"
echo ""
echo " Reinnoire automata: certbot verifica la fiecare 12h"
echo "======================================================================"
