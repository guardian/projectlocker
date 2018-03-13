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
        if(this.state.content)
            return <span>{this.state.content.title} ({this.state.content.siteId}-{this.state.content.collectionId})</span>
        else
            return <span><i>(none)</i></span>
    }
}

export default CommissionEntryView;