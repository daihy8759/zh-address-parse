const { createLogger, format, transports } = require('winston');
const {combine, timestamp, label, printf} = format;

const myFormat = printf(({level, message, label, timestamp}) => {
  return `${timestamp} [${label}] ${level}: ${message}`;
});

const logger = createLogger({
  level: 'info',
  format: combine(
      label({label: 'zh-address-parse'}),
      timestamp(),
      myFormat
  ),
  transports: [
    new (transports.Console)(),
    new (transports.File)({filename: 'parse.log'})
  ]
});

module.exports = logger;