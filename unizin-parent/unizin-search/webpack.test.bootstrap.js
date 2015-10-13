const context = require.context("./src", true, /__test__\/\S+\.jsx?$/)
context.keys().forEach(context)
