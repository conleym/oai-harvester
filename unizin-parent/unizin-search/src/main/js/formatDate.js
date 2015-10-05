import moment from 'moment'

export function formatDate(date) {

    if ( typeof date === 'string' ) {
        return formatString(date)
    } else {
        return date.map(d => formatString(d))
    }
}

function formatString(date) {
    return moment(date).format('MMMM D, YYYY')
}
