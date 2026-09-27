// No mongosh, selecione antes o banco indicado pela demonstração:
// use produto1_demo_<identificador>
// Para os dados do menu interativo: use produto1_academico

db.cursos.find();
db.turmas.find();
db.periodos.find();
db.alunos.find();
db.desafios.find();

// No banco da demonstração há exatamente um desafio principal.
const desafio = db.desafios.findOne();
if (desafio) {
  print('Desafio consultado pelo identificador:');
  printjson(db.desafios.findOne({ _id: desafio._id }));
  print('Alunos da turma:');
  db.alunos.find({ turma: desafio.turma }).forEach(printjson);
  print('Alunos do desafio:');
  db.alunos.find({ _id: { $in: desafio.alunos } }).forEach(printjson);
}

// O índice ra_1 deve apresentar unique: true.
db.alunos.getIndexes();
