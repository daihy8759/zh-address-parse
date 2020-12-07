const fastify = require('fastify')({
  logger: true,
  disableRequestLogging: true
});
const AddressParser = require('./address-parser');
const logger = require('./log');

fastify.post('/parse', async (req) => {
  const body = req.body;
  if (!body.address) {
    return {status: 400, message: '地址有误！'}
  }
  logger.info(`解析地址：${body.address}`);
  const options = JSON.parse(body.options || "{}");
  const parseFields = options.parseFields ? JSON.parse(options.parseFields) : {
    name: true,
    phoneNo: true,
    postalCode: true
  }
  console.log(parseFields)
  const data = AddressParser(body.address, {
    type: 0,
    parseFields,
    textFilter: [],
    nameMaxLength: 4,
  });
  return {status: 200, data: data}
})

const start = async () => {
  try {
    await fastify.listen(3000)
    fastify.log.info(`server listening on ${fastify.server.address().port}`)
  } catch (err) {
    fastify.log.error(err)
    process.exit(1)
  }
}
start()