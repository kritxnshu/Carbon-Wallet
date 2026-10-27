-- Vehicles: an optional GPS / connected-car unit ID, and the PUC certificate the family adds after the sale
ALTER TABLE "vehicles" ADD COLUMN "gps_device_id" TEXT;
ALTER TABLE "vehicles" ADD COLUMN "puc_number" TEXT;
ALTER TABLE "vehicles" ADD COLUMN "puc_valid_until" TIMESTAMP(3);
ALTER TABLE "vehicles" ADD COLUMN "puc_source" TEXT;
ALTER TABLE "vehicles" ADD COLUMN "puc_added_at" TIMESTAMP(3);
