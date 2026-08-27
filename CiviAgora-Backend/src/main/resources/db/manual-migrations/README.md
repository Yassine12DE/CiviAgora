# Database migrations

This repository currently uses Hibernate schema updates and does not include an automated migration runner.
The SQL in this directory is the reviewed MySQL deployment migration for the organization product upgrade.
Apply it once per environment before switching `spring.jpa.hibernate.ddl-auto` away from `update`.
