import { smartLoader } from 'unizin-js-tools/dist/index.js'
import Loader from '../components/loading.jsx'
import { connect } from 'react-redux'

export default smartLoader(connect, Loader)
