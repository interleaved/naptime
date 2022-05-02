-- simple database for testing
create extension pgcrypto;
--;;
create extension "uuid-ossp";
--;;
create table professor (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  first_name text not null,
  last_name text not null
);
--;;
create table class (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title text not null,
  num int not null,
  hours int not null,
  professor UUID NOT NULL,
  CONSTRAINT class_professor_key FOREIGN KEY (professor) REFERENCES professor(id)
);
--;;
create table material (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name text not null,
  price money not null
);
--;;
create table class_material (
  class_id UUID,
  material_id UUID,
  CONSTRAINT class_material_class_key FOREIGN KEY (class_id) REFERENCES class(id),
  CONSTRAINT class_material_material_key FOREIGN KEY (material_id) REFERENCES material(id)
)
--;;
create table student (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  first_name text not null,
  last_name text not null
);
--;;
create table class_student (
  class_id UUID,
  student_id UUID,
  CONSTRAINT class_student_class_key FOREIGN KEY (class_id) REFERENCES class(id),
  CONSTRAINT class_student_student_key FOREIGN KEY (student_id) REFERENCES student(id)
)
--;;
