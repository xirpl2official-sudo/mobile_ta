# Bugfix Requirements Document

## Introduction

Halaman "Siswa Belum Terdaftar" saat ini menggunakan tabel horizontal dengan 7 kolom yang dirancang untuk desktop. Pada layar mobile (360dp-430dp), layout tabel menyebabkan pengalaman pengguna yang buruk: informasi terpotong, tombol "Detail" ditampilkan secara vertikal, kolom terlalu sempit, dan layout terlihat tidak profesional. Bug ini memengaruhi semua pengguna admin yang mengakses halaman ini dari perangkat mobile Android.

Perbaikan ini akan mengubah layout dari tabel horizontal menjadi Material Card List yang mobile-friendly, mengikuti best practices Material Design 3 untuk layar kecil, sambil mempertahankan semua fungsionalitas yang ada (search, filter, pagination, multi-select, dan notifikasi).

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN the page is displayed on mobile screens (360dp-430dp width) THEN the system displays a horizontal table with 7 columns that is too wide, causing horizontal scrolling and poor readability

1.2 WHEN the "Detail" button is rendered in the narrow column THEN the system displays the button text vertically (character by character) instead of horizontally, making it look unprofessional

1.3 WHEN displaying NIS, Nama, Jurusan, and Wali Kelas fields in narrow columns THEN the system truncates the text with ellipsis (...), hiding important information

1.4 WHEN the user attempts to tap checkboxes, buttons, or dropdown filters THEN the system provides tap targets that are too small (<48dp), making interaction difficult

1.5 WHEN the page layout is rendered on mobile THEN the system displays the desktop-optimized table without responsive adaptation, resulting in cramped spacing and poor visual hierarchy

1.6 WHEN the user views the list of students THEN the system presents all information with equal visual weight, making it difficult to identify the most important information (student name)

1.7 WHEN the user attempts to use the Jurusan and Wali Kelas dropdown filters THEN the system displays dropdown controls that are too small (40dp height with 11sp text), making them difficult to tap and read

### Expected Behavior (Correct)

2.1 WHEN the page is displayed on mobile screens (360dp-430dp width) THEN the system SHALL display each student in a MaterialCardView with vertical information layout, eliminating horizontal scrolling and improving readability

2.2 WHEN the "Detail" button is rendered within the card THEN the system SHALL display the button with horizontal text and a minimum height of 40dp with full width or end-aligned positioning

2.3 WHEN displaying NIS, Nama, Jurusan, and Wali Kelas fields in the card THEN the system SHALL show complete information without truncation, using appropriate typography hierarchy

2.4 WHEN the user attempts to tap checkboxes, buttons, or interactive elements THEN the system SHALL provide tap targets of at least 48dp, following Material Design accessibility guidelines

2.5 WHEN the page layout is rendered on mobile THEN the system SHALL use a card-based RecyclerView layout optimized for 360dp-430dp screens with proper spacing (8dp margins, 16dp padding, 12dp element spacing)

2.6 WHEN the user views the list of students THEN the system SHALL display student names in bold 16sp typography with primary color, while secondary information (NIS, Kelas, Jurusan, Wali Kelas) uses smaller font sizes (14sp, 12sp) with secondary colors, creating clear visual hierarchy

2.7 WHEN the user attempts to use the Jurusan and Wali Kelas dropdown filters THEN the system SHALL display dropdown controls with larger touch targets (minimum 48dp height) and readable text size (14sp minimum), making them easy to tap and read

### Unchanged Behavior (Regression Prevention)

3.1 WHEN the user enters text in the search field THEN the system SHALL CONTINUE TO filter the student list by student name with debounced search

3.2 WHEN the user selects a Jurusan from the dropdown filter THEN the system SHALL CONTINUE TO filter the list to show only students from that Jurusan

3.3 WHEN the user selects a Wali Kelas from the dropdown filter THEN the system SHALL CONTINUE TO filter the list to show only students assigned to that Wali Kelas

3.4 WHEN the displayed list exceeds 20 students THEN the system SHALL CONTINUE TO paginate the results with 20 items per page using client-side pagination

3.5 WHEN the user taps the checkbox on a student card THEN the system SHALL CONTINUE TO toggle the selection state and update the selected count

3.6 WHEN the user taps the "Select All" checkbox THEN the system SHALL CONTINUE TO select or deselect all students on the current page

3.7 WHEN one or more students are selected THEN the system SHALL CONTINUE TO display the notification bar with selected count and "Kirim Notifikasi" button

3.8 WHEN the user taps the "Kirim Notifikasi" button THEN the system SHALL CONTINUE TO send notifications to the Wali Kelas of selected students

3.9 WHEN the user taps the "Detail" button on a student card THEN the system SHALL CONTINUE TO open the detail dialog showing complete student information

3.10 WHEN the user performs a pull-to-refresh gesture THEN the system SHALL CONTINUE TO reload the student data from the API

3.11 WHEN the filtered/searched result returns no students THEN the system SHALL CONTINUE TO display the empty state with icon and message "Data tidak ditemukan"

3.12 WHEN data is being loaded from the API THEN the system SHALL CONTINUE TO display the progress bar indicator

3.13 WHEN the adapter binds student data to a card THEN the system SHALL CONTINUE TO format NIS styling (italic gray for temp IDs with "/" or ".", normal blue for valid NIS)

3.14 WHEN the adapter binds student data to a card THEN the system SHALL CONTINUE TO display "-" as placeholder for empty fields (kelas, jurusan, waliKelasName)

3.15 WHEN the page is initialized or filters are changed THEN the system SHALL CONTINUE TO reset pagination to page 1
