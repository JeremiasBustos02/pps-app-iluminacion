-- V4__seed_tipo_reclamo.sql
INSERT INTO tipo_reclamo (nombre, prioridad, created_at, updated_at) VALUES
                                                                         ('Luminaria apagada', 2, now(), now()),
                                                                         ('Parpadeo intermitente', 1, now(), now()),
                                                                         ('Caída de rama', 2, now(), now()),
                                                                         ('Corte de luz general', 3, now(), now()),
                                                                         ('Poste dañado o caído', 3, now(), now()),
                                                                         ('Cableado expuesto', 3, now(), now()),
                                                                         ('Otro', 1, now(), now());