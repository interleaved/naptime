-- simple database for testing
create extension pgcrypto;
--;;
create extension "uuid-ossp";
--;;
create table address (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  address text not null
);
--;;
create table professor (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  first_name text not null,
  last_name text not null,
  address UUID,
  CONSTRAINT professor_address_key FOREIGN KEY (address) REFERENCES address(id)
);
--;;
create table class (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title text not null,
  num int not null,
  hours int not null,
  professor UUID,
  CONSTRAINT class_professor_key FOREIGN KEY (professor) REFERENCES professor(id)
);
--;;
create table material (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name text not null,
  author text,
  price money not null
);
--;;
create table class_material (
  class UUID,
  material UUID,
  CONSTRAINT class_material_class_key FOREIGN KEY (class) REFERENCES class(id),
  CONSTRAINT class_material_material_key FOREIGN KEY (material) REFERENCES material(id)
)
--;;
create table student (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  first_name text not null,
  last_name text not null,
  billing_address UUID,
  shipping_address UUID,
  CONSTRAINT student_billing_address_key FOREIGN KEY (billing_address) REFERENCES address(id),
  CONSTRAINT student_shipping_address_key FOREIGN KEY (shipping_address) REFERENCES address(id)
);
--;;
create table class_student (
  class UUID,
  student UUID,
  CONSTRAINT class_student_class_key FOREIGN KEY (class) REFERENCES class(id),
  CONSTRAINT class_student_student_key FOREIGN KEY (student) REFERENCES student(id)
)
--;;
