// uniqueness constrains (implying an index)
// static nodes
CREATE CONSTRAINT ON (n:Country)      ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:City)         ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Tag)          ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:TagClass)     ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Organisation) ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:University)   ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Company)      ASSERT n.id IS UNIQUE;
// dynamic nodes
CREATE CONSTRAINT ON (n:Message)      ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Comment)      ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Post)         ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Forum)        ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Person)       ASSERT n.id IS UNIQUE;

// name/firstName
CREATE INDEX ON :Country(name);
CREATE INDEX ON :Person(firstName);
CREATE INDEX ON :Tag(name);
CREATE INDEX ON :TagClass(name);

// creationDate
CREATE INDEX ON :Message(creationDate);
CREATE INDEX ON :Comment(creationDate);
CREATE INDEX ON :Post(creationDate);
CREATE INDEX ON :Forum(creationDate);
CREATE INDEX ON :Person(creationDate);