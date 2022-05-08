create view prof as
  select first_name, last_name,
    concat_ws(' ', first_name, last_name) as full_name,
    sum(c.hours) as total_hours,
    a.address as address
  from professor p
    left join address a on a.id = p.address
    left join class c on c.professor = p.id
  group by p.first_name, p.last_name, a.address;
