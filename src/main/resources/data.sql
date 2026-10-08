-- Seed doctors (taken from the frontend db.json). INSERT IGNORE = safe to re-run.
INSERT IGNORE INTO doctors (id, name, specialization, experience, fees, location, image, status, rating) VALUES
(1,  'Dr CHANDRU', 'Cardiologist',      10, 700, 'Chennai',    '/img1.jpg',  'active', 4.8),
(2,  'Dr GAUTAM',  'Dermatologist',      7, 400, 'Chennai',    '/img2.jpg',  'active', 4.6),
(3,  'Dr INYAN',   'Neurologist',        8, 600, 'Bangalore',  '/img3.jpg',  'active', 4.7),
(4,  'Dr GOKUL',   'Pediatrician',       6, 450, 'Salem',      '/img13.jpg', 'active', 4.5),
(5,  'Dr SABEEK',  'Orthopedic',         9, 550, 'Hosur',      '/img12.jpg', 'active', 4.6),
(6,  'Dr RAMESH',  'ENT Specialist',    12, 500, 'Coimbatore', '/img22.jpg', 'active', 4.7),
(7,  'Dr PRIYA',   'Gynecologist',      11, 650, 'Chennai',    '/img23.jpg', 'active', 4.9),
(8,  'Dr ARJUN',   'Psychiatrist',       5, 700, 'Bangalore',  '/img24.jpg', 'active', 4.5),
(9,  'Dr KAVIN',   'Dentist',            4, 350, 'Hosur',      '/img26.jpg', 'active', 4.4),
(10, 'Dr SOPHIE',  'General Physician', 13, 500, 'Chennai',    '/img27.jpg', 'active', 4.8);

-- Fill the new profile columns for the seeded doctors (idempotent: only touches rows that are still empty).
UPDATE doctors SET qualification = 'MBBS, MD, DM (Cardiology)', languages = 'English, Tamil',          available_days = 'Mon-Sat', about = 'Senior cardiologist focused on preventive heart care, hypertension and cardiac rehabilitation.'  WHERE id = 1  AND qualification IS NULL;
UPDATE doctors SET qualification = 'MBBS, MD (Dermatology)',        languages = 'English, Tamil, Hindi', available_days = 'Mon-Fri', about = 'Treats acne, eczema, pigmentation and hair loss with evidence-based dermatology.'                  WHERE id = 2  AND qualification IS NULL;
UPDATE doctors SET qualification = 'MBBS, DM (Neurology)',          languages = 'English, Kannada',      available_days = 'Mon-Sat', about = 'Specialises in migraine, epilepsy, stroke recovery and nerve disorders.'                                WHERE id = 3  AND qualification IS NULL;
UPDATE doctors SET qualification = 'MBBS, MD (Pediatrics)',         languages = 'English, Tamil',        available_days = 'Mon-Sat', about = 'Gentle child care from newborn checkups to adolescent health and vaccinations.'                         WHERE id = 4  AND qualification IS NULL;
UPDATE doctors SET qualification = 'MBBS, MS (Orthopaedics)',       languages = 'English, Tamil',        available_days = 'Mon-Fri', about = 'Joint, bone and sports injury care including fracture management and rehabilitation.'                 WHERE id = 5  AND qualification IS NULL;
UPDATE doctors SET qualification = 'MBBS, MS (ENT)',                languages = 'English, Tamil',        available_days = 'Mon-Sat', about = 'Ear, nose and throat specialist for sinus, hearing and voice problems.'                                WHERE id = 6  AND qualification IS NULL;
UPDATE doctors SET qualification = 'MBBS, MS (Obstetrics & Gynaecology)', languages = 'English, Tamil, Hindi', available_days = 'Mon-Sat', about = 'Women''s health, pregnancy care and minimally invasive gynaecology.'                              WHERE id = 7  AND qualification IS NULL;
UPDATE doctors SET qualification = 'MBBS, MD (Psychiatry)',         languages = 'English, Kannada, Hindi', available_days = 'Tue-Sat', about = 'Compassionate care for anxiety, depression, sleep and stress-related conditions.'                    WHERE id = 8  AND qualification IS NULL;
UPDATE doctors SET qualification = 'BDS, MDS (Conservative Dentistry)', languages = 'English, Tamil',    available_days = 'Mon-Sat', about = 'Routine dental care, root canals, cosmetic dentistry and oral hygiene.'                                WHERE id = 9  AND qualification IS NULL;
UPDATE doctors SET qualification = 'MBBS, MD (General Medicine)',   languages = 'English, Tamil',        available_days = 'Mon-Sun', about = 'First point of contact for fever, infections, lifestyle disease and general health checkups.'            WHERE id = 10 AND qualification IS NULL;
