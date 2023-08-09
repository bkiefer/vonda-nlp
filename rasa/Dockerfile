FROM rasa/rasa:3.5.14-full

WORKDIR /app
COPY domain.yml /app/
COPY config.yml /app/
COPY models/ /app/models/

EXPOSE 5005
CMD ["run", "--enable-api", "--port", "5005"]