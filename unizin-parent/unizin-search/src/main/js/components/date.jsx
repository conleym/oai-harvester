import React from 'react'
import moment from 'moment'
import Time from './time.jsx'

const { any } = React.PropTypes

export default class Date extends React.Component {

  static displayName = 'Date'

  static propTypes = {
      date: any.isRequired,
  }

  constructor(props, context) {
      super(props, context)

      // dates will come back as either a string or an array
      // right now we are only bringing back the first date of an array set
      this.date = (typeof this.props.date === 'string') ? this.props.date : this.props.date[0]
  }

  formatString(date) {
      // in some cases we won't get a date back.
      // for examplethere might be a string representation for the dc.date.avaiable
      // http://deepblue.lib.umich.edu/handle/2027.42/55391?show=full
      // in that case we'll just return the string instead of the formatted date

      if (moment(date).isValid()) {
          return moment(date).format('MMMM D, YYYY')
      } else {
          return date
      }
  }

  render() {
      return <Time formattedDate={this.formatString(this.date)} dateTime={this.date} />
  }
}
