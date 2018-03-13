import GenericEntryView from './GenericEntryView.jsx';
import PropTypes from 'prop-types';

class WorkingGroupEntryView extends GenericEntryView {
    static propTypes = {
        entryId: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);
        this.endpoint = "/api/pluto/workinggroup"
    }
    
}

export default WorkingGroupEntryView;