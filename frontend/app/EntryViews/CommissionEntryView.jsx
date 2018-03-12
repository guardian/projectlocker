import GenericEntryView from './GenericEntryView.jsx';
import PropTypes from 'prop-types';

class CommissionEntryView extends GenericEntryView {
    static propTypes = {
        entryId: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);
        this.endpoint = "/api/pluto/commission"
    }

    render(){
        return <span>{this.state.content.title} ({this.state.content.siteId}-{this.state.content.collectionId})</span>
    }
}

export default CommissionEntryView;