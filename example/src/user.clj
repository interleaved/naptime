(ns user
  (:require [naptime.db :as db]
            [naptime.model :as model]
            [naptime.core :as nap]
            [clojure.pprint :as pp]
            [next.jdbc.sql :as sql]
            [conman.core :as conman]
            [mount.core :as mount :refer [defstate]]))

(def pool-spec {:jdbc-url "jdbc:postgresql://localhost/university?user=postgres&password=password"})

(defstate ^:dynamic *db*
  :start (conman/connect! pool-spec)
  :stop (conman/disconnect! *db*))

(defstate queries
  :start (nap/query-map *db*))

(defn gen-professor []
  {:first_name (rand-nth first-names)
   :last-name (rand-nth surnames)})

(def surnames
  ["Smith"
   "Johnson"
   "Williams"
   "Brown"
   "Jones"
   "Garcia"
   "Miller	"
   "Davis"
   "Rodriguez"
   "Martinez"
   "Hernandez"
   "Lopez"
   "Gonzales"
   "Wilson"
   "Anderson"
   "Thomas"
   "Taylor"
   "Moore"
   "Jackson"
   "Martin"
   "Lee"
   "Perez"
   "Thompson"
   "White"
   "Harris"
   "Sanchez"
   "Clark"
   "Ramirez"
   "Lewis"
   "Robinson"
   "Walker"
   "Young"
   "Allen"
   "King"
   "Wright"
   "Scott"
   "Torres"
   "Nguyen"
   "Hill"
   "Flores"
   "Green"
   "Adams"
   "Nelson"
   "Baker"
   "Hall"
   "Rivera"
   "Campbell"
   "Mitchell"
   "Carter"
   "Roberts"
   "Gomez"
   "Phillips"
   "Evans"
   "Turner"
   "Diaz"
   "Parker"
   "Cruz"
   "Edwards"
   "Collins"
   "Reyes"
   "Stewart"
   "Morris"
   "Morales"
   "Murphy"
   "Cook"
   "Rogers"
   "Gutierrez"
   "Ortiz"
   "Morgan"
   "Cooper"
   "Peterson"
   "Bailey"
   "Reed"
   "Kelly"
   "Howard"
   "Ramos"
   "Kim"
   "Cox"
   "Ward"
   "Richardson"
   "Watson"
   "Brooks"
   "Chavez"
   "Wood"
   "James"
   "Bennet"
   "Gray"
   "Mendoza"
   "Ruiz"
   "Hughes"
   "Price"
   "Alvarez"
   "Castillo"
   "Sanders"
   "Patel"
   "Myers"
   "Long"
   "Ross"
   "Foster"
   "Jimenez"])

(def first-names
  ["James" "Mary"
   "Robert" "Patricia"
   "John" "Jennifer"
   "Michael" "Linda"
   "William" "Elizabeth"
   "David" "Barbara"
   "Richard" "Susan"
   "Joseph" "Jessica"
   "Thomas" "Sarah"
   "Charles" "Karen"
   "Christopher" "Nancy"
   "Daniel" "Lisa"
   "Matthew" "Betty"
   "Anthony" "Margaret"
   "Mark" "Sandra"
   "Donald" "Ashley"
   "Steven" "Kimberly"
   "Paul" "Emily"
   "Andrew" "Donna"
   "Joshua" "Michelle"
   "Kenneth" "Dorothy"
   "Kevin" "Carol"
   "Brian" "Amanda"
   "George" "Melissa"
   "Edward" "Deborah"
   "Ronald" "Stephanie"
   "Timothy" "Rebecca"
   "Jason" "Sharon"
   "Jeffrey" "Laura"
   "Ryan" "Cynthia"
   "Jacob" "Kathleen"
   "Gary" "Amy"
   "Nicholas" "Shirley"
   "Eric" "Angela"
   "Jonathan" "Helen"
   "Stephen" "Anna"
   "Larry" "Brenda"
   "Justin" "Pamela"
   "Scott" "Nicole"
   "Brandon" "Emma"
   "Benjamin" "Samantha"
   "Samuel" "Katherine"
   "Gregory" "Christine"
   "Frank" "Debra"
   "Alexander" "Rachel"
   "Raymond" "Catherine"
   "Patrick" "Carolyn"
   "Jack" "Janet"
   "Dennis" "Ruth"
   "Jerry" "Maria"
   "Tyler" "Heather"
   "Aaron" "Diane"
   "Jose" "Virginia"
   "Adam" "Julie"
   "Henry" "Joyce"
   "Nathan" "Victoria"
   "Douglas" "Olivia"
   "Zachary" "Kelly"
   "Peter" "Christina"
   "Kyle" "Lauren"
   "Walter" "Joan"
   "Ethan" "Evelyn"
   "Jeremy" "Judith"
   "Harold" "Megan"
   "Keith" "Cheryl"
   "Christian" "Andrea"
   "Roger" "Hannah"
   "Noah" "Martha"
   "Gerald" "Jacqueline"
   "Carl" "Frances"
   "Terry" "Gloria"
   "Sean" "Ann"
   "Austin" "Teresa"
   "Arthur" "Kathryn"
   "Lawrence" "Sara"
   "Jesse" "Janice"
   "Dylan" "Jean"
   "Bryan" "Alice"
   "Joe" "Madison"
   "Jordan" "Doris"
   "Billy" "Abigail"
   "Bruce" "Julia"
   "Albert" "Judy"
   "Willie" "Grace"
   "Gabriel" "Denise"
   "Logan" "Amber"
   "Alan" "Marilyn"
   "Juan" "Beverly"
   "Wayne" "Danielle"
   "Roy" "Theresa"
   "Ralph" "Sophia"
   "Randy" "Marie"
   "Eugene" "Diana"
   "Vincent" "Brittany"
   "Russell" "Natalie"
   "Elijah" "Isabella"
   "Louis" "Charlotte"
   "Bobby" "Rose"
   "Philip" "Alexis"
   "Johnny" "Kayla"])

(def material
  [{:book "Things Fall Apart" :author "Chinua Achebe"}
   {:book "Fairy tales" :author "Hans Christian Andersen"}
   {:book "The Divine Comedy" :author "Dante Alighieri"}
   {:book "Epic of Gilgamesh" :author "Unknown"}
   {:book "Book of Job" :author "Unknown"}
   {:book "One Thousand and One Nights" :author "Various"}
   {:book "Njál's Saga" :author "Unknown, possibly Sæmundr fróði"}
   {:book "Pride and Prejudice" :author "Jane Austen"}
   {:book "Le Père Goriot" :author "Honoré de Balzac"}
   {:book "Molloy, Malone Dies, The Unnamable, a trilogy" :author "Samuel Beckett"}
   {:book "The Decameron" :author "Giovanni Boccaccio"}
   {:book "Ficciones" :author "Jorge Luis Borges"}
   {:book "Wuthering Heights" :author "Emily Brontë"}
   {:book "The Stranger" :author "Albert Camus"}
   {:book "Poems" :author "Paul Celan"}
   {:book "Journey to the End of the Night" :author "Louis-Ferdinand Céline"}
   {:book "Don Quixote" :author "Miguel de Cervantes"}
   {:book "The Canterbury Tales" :author "Geoffrey Chaucer"}
   {:book "Stories" :author "Anton Chekhov"}
   {:book "Nostromo" :author "Joseph Conrad"}
   {:book "Great Expectations" :author "Charles Dickens"}
   {:book "Jacques the Fatalist" :author "Denis Diderot"}
   {:book "Berlin Alexanderplatz" :author "Alfred Döblin"}
   {:book "Crime and Punishment" :author "Fyodor Dostoevsky"}
   {:book "The Idiot" :author "Fyodor Dostoevsky"}
   {:book "Demons" :author "Fyodor Dostoevsky"}
   {:book "The Brothers Karamazov" :author "Fyodor Dostoevsky"}
   {:book "Middlemarch" :author "George Eliot"}
   {:book "Invisible Man" :author "Ralph Ellison"}
   {:book "Medea" :author "Euripides"}
   {:book "Absalom, Absalom!" :author "William Faulkner"}
   {:book "The Sound and the Fury" :author "William Faulkner"}
   {:book "Madame Bovary" :author "Gustave Flaubert"}
   {:book "Sentimental Education" :author "Gustave Flaubert"}
   {:book "Gypsy Ballads" :author "Federico García Lorca"}
   {:book "One Hundred Years of Solitude" :author "Gabriel García Márquez"}
   {:book "Love in the Time of Cholera" :author "Gabriel García Márquez"}
   {:book "Faust" :author "Johann Wolfgang von Goethe"}
   {:book "Dead Souls" :author "Nikolai Gogol"}
   {:book "The Tin Drum" :author "Günter Grass"}
   {:book "The Devil to Pay in the Backlands" :author "João Guimarães Rosa"}
   {:book "Hunger" :author "Knut Hamsun"}
   {:book "The Old Man and the Sea" :author "Ernest Hemingway"}
   {:book "Iliad" :author "Homer"}
   {:book "Odyssey" :author "Homer"}
   {:book "A Doll's House" :author "Henrik Ibsen"}
   {:book "Ulysses" :author "James Joyce"}
   {:book "Stories" :author "Franz Kafka"}
   {:book "The Trial" :author "Franz Kafka"}
   {:book "The Castle" :author "Franz Kafka"}
   {:book "Shakuntala" :author "Kālidāsa"}
   {:book "The Sound of the Mountain" :author "Yasunari Kawabata"}
   {:book "Zorba the Greek" :author "Nikos Kazantzakis"}
   {:book "Sons and Lovers" :author "D. H. Lawrence"}
   {:book "Independent People" :author "Halldór Laxness"}
   {:book "Canti" :author "Giacomo Leopardi"}
   {:book "The Golden Notebook" :author "Doris Lessing"}
   {:book "Pippi Longstocking" :author "Astrid Lindgren"}
   {:book "A Madman's Diary" :author "Lu Xun"}
   {:book "Children of Gebelawi" :author "Naguib Mahfouz"}
   {:book "Buddenbrooks" :author "Thomas Mann"}
   {:book "The Magic Mountain" :author "Thomas Mann"}
   {:book "Moby-Dick" :author "Herman Melville"}
   {:book "Essays" :author "Michel de Montaigne"}
   {:book "History" :author "Elsa Morante"}
   {:book "Beloved" :author "Toni Morrison"}
   {:book "The Tale of Genji" :author "Murasaki Shikibu"}
   {:book "The Man Without Qualities" :author "Robert Musil"}
   {:book "Lolita" :author "Vladimir Nabokov"}
   {:book "Nineteen Eighty-Four" :author "George Orwell"}
   {:book "Metamorphoses" :author "Ovid"}
   {:book "The Book of Disquiet" :author "Fernando Pessoa"}
   {:book "Tales" :author "Edgar Allan Poe"}
   {:book "In Search of Lost Time" :author "Marcel Proust"}
   {:book "Gargantua and Pantagruel" :author "François Rabelais"}
   {:book "Pedro Páramo" :author "Juan Rulfo"}
   {:book "Masnavi" :author "Rumi"}
   {:book "Midnight's Children" :author "Salman Rushdie"}
   {:book "Bostan" :author "Saadi"}
   {:book "Season of Migration to the North" :author "Tayeb Salih"}
   {:book "Blindness" :author "José Saramago"}
   {:book "Hamlet" :author "William Shakespeare"}
   {:book "King Lear" :author "William Shakespeare"}
   {:book "Othello" :author "William Shakespeare"}
   {:book "Oedipus the King" :author "Sophocles"}
   {:book "The Red and the Black" :author "Stendhal"}
   {:book "Tristram Shandy" :author "Laurence Sterne"}
   {:book "Confessions of Zeno" :author "Italo Svevo"}
   {:book "Gulliver's Travels" :author "Jonathan Swift"}
   {:book "War and Peace" :author "Leo Tolstoy"}
   {:book "Anna Karenina" :author "Leo Tolstoy"}
   {:book "The Death of Ivan Ilyich" :author "Leo Tolstoy"}
   {:book "Adventures of Huckleberry Finn" :author "Mark Twain"}
   {:book "Ramayana" :author "Valmiki"}
   {:book "Aeneid" :author "Virgil"}
   {:book "Mahabharata" :author "Vyasa"}
   {:book "Leaves of Grass" :author "Walt Whitman"}
   {:book "Mrs Dalloway" :author "Virginia Woolf"}
   {:book "To the Lighthouse" :author "Virginia Woolf"}
   {:book "Memoirs of Hadrian" :author "Marguerite Yourcenar"}])
