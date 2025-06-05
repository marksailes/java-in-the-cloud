CREATE TABLE car_charge (
    charge_id UUID NOT NULL,
    car_id UUID NOT NULL,
    supplier VARCHAR(255) NOT NULL,
    amount INTEGER NOT NULL,
    PRIMARY KEY (charge_id)
);