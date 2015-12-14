var AWS = require('aws-sdk');
var SQS = new AWS.SQS();


function verifyQueues(queues, context) {
  if (queues == null) {
    context.fail('`queues` is null or undefined.');
    return false;
  }
  if (!Array.isArray(queues)) {
    context.fail('`queues` is not an array.');
    return false;
  }
  if (queues.length === 0) {
    context.succeed('`queues` is empty -- nothing to do.');
    return false;
  }
  return true;
}


function verifyEvent(event, context) {
  if (event == null) {
    context.fail('`event` is null or undefined.');
    return false;
  }
  if (!Array.isArray(event.Records)) {
    context.fail('`event.Records` is not an array.');
    return false;
  }
  if (event.Records.length === 0) {
    context.succeed('No records in event -- nothing to do.');
    return false;
  }
  return true;
}


function makeSQSCallback(context, status) {
  return function(err, data) {
    if (err) {
      status.failures++;
      console.warn(err, err.stack);
    } else {
      status.successes++;
      console.log(data);
    }
  };
}


function getValue(item, value, type) {
    if (item == null) {
        return null;
    }
    type = type = (type == null) ? 'S' : type;
    var first = item[value];
    return (first == null) ? null : first[type];
}


function getXML(item) {
    return getValue(item, 'XML', 'B');
}

function getChecksum(item) {
    return getValue(item, 'XMLChecksum', 'B');
}


function verifyNewItem(newItem) {
  console.log('Verifying item: ', newItem);
  var valid = true;
  if (getXML(newItem) == null) {
    console.log('Missing XML.');
    valid = false;
  }
  if (getValue(newItem, 'BaseUrl') == null) {
    console.log('Missing BaseUrl.');
    valid = false;
  }
  if (getValue(newItem, 'Identifier') == null) {
    console.log('Missing Identifier.');
    valid = false;
  }
  return valid;
}


function maybeSend(queues, record, sqsCallback) {
  console.log('Checking record: ', record);
  if (record.eventSource !== 'aws:dynamodb') {
    console.warn('Not a DynamoDB event -- skipping record.');
    return;
  }

  var oldItem = record.dynamodb.OldImage;
  var newItem = record.dynamodb.NewImage;
  if (newItem == null) {
    console.warn('No new item -- skipping record.');
    return;
  }
  if (!verifyNewItem(newItem)) {
    console.warn('New item is invalid -- skipping record.');
    return;
  }

  var sendThis = true;
  var oldChecksum = getChecksum(oldItem);
  var newChecksum = getChecksum(newItem);
  if (newChecksum == null) {
    console.warn('New item has no checksum -- skipping record.');
    sendThis = false;
  }
  if (oldChecksum === newChecksum) {
    console.log('Old and new checksums match -- skipping record.');
    sendThis = false;
  }
  if (sendThis) {
    console.log("Sending record to nuxeo.");
    send(queues, newItem, sqsCallback);
  }
}


function send(queues, newItem, sqsCallback) {
  var xml = getXML(newItem);
  var id = getValue(newItem, 'Identifier');
  var url = getValue(newItem, 'BaseUrl');
  for (var i = 0; i < queues.length; i++) {
    var msg = {
      QueueUrl: queues[i],
      MessageBody: xml,
      DelaySeconds: 0,
      MessageAttributes: {
        Identifier: {
          DataType: "String",
          StringValue: id
        },
        BaseUrl: {
          DataType: "String",
          StringValue: url
        }
      }
    };
    console.log('Sending message: ', msg);
    SQS.sendMessage(msg, sqsCallback);
  }
}


exports.handler = function(arg, context) {
  var queues = arg.queues;
  if (!verifyQueues(queues, context)) {
    return;
  }

  var event = arg.event;
  if (!verifyEvent(event, context)) {
    return;
  }

  var nevents = event.Records.length;
  var status = {'successes': 0, 'failures': 0};
  var sqsCallback = makeSQSCallback(context, status);
  for (var i = 0; i < nevents; i++) {
    // Break multiple-event messages into multiple single-event messages
    // to avoid creating messages that exceed size limits.
    maybeSend(queues, event.Records[i], sqsCallback);
  }
  context.succeed('Tried to send ' + nevents + '. Successes: ' +
    status.successes + '. Failures: ' + status.failures + '.');
};
