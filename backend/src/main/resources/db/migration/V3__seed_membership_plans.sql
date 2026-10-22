-- V3: starter membership plans (sample data for development; edit before real use)

INSERT INTO membership_plan (name, description, duration_days, price) VALUES
    ('Monthly',   'Gym floor access, 1 month.',                                        30,  1500.00),
    ('Quarterly', 'Gym floor access plus group classes, 3 months.',                    90,  4000.00),
    ('Half-Yearly', 'Gym floor access, group classes and 2 trainer sessions, 6 months.', 180, 7000.00),
    ('Annual',    'Everything included, 12 months, best value.',                      365, 12000.00);
