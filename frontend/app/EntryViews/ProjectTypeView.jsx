import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';
import GenericEntryView from './GenericEntryView.jsx';

class ProjectTypeView extends GenericEntryView {
    static propTypes = {
        entryId: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);
        this.endpoint = "/api/projecttype"
    }

    render(){
        return <span>{this.state.content.name}</span>
    }
}

export default ProjectTypeView;
