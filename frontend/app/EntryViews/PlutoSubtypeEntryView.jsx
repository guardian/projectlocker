import React from 'react';
import GenericEntryView from './GenericEntryView.jsx';
import PropTypes from 'prop-types';

class PlutoSubtypeEntryView extends GenericEntryView {
    static propTypes = {
        entryId: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);
        this.endpoint = "/api/plutoprojecttypeid"
    }

    render(){
        if(this.state.content==={} || this.props.entryId==undefined) return <span className="value-not-present">(none)</span>;

        return this.state.content ?
            <span>{this.state.content.name} ({this.state.content.uuid})</span> :
            <span>loading...</span>
    }
}

export default PlutoSubtypeEntryView;
