-- Увеличиваем размер поля file_name с 30 до 255 символов
ALTER TABLE file.files ALTER COLUMN file_name TYPE VARCHAR(255);

-- Увеличиваем размер поля mime_type с 64 до 128 символов
ALTER TABLE file.files ALTER COLUMN mime_type TYPE VARCHAR(128);

-- Обновляем историческую таблицу для аудита
ALTER TABLE file_history.files_history ALTER COLUMN file_name TYPE VARCHAR(255);
ALTER TABLE file_history.files_history ALTER COLUMN mime_type TYPE VARCHAR(128);

