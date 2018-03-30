import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

class PlutoProjectTypeSelector extends React.Component {
    static propTypes = {
        id: PropTypes.string.isRequired,
        plutoProjectTypesList: PropTypes.array.isRequired,
        selectionUpdated: PropTypes.func.isRequired,
        selectedType: PropTypes.number.isRequired,
        subTypesFor: PropTypes.number,
        onlyShowSubtypes: PropTypes.boolean
    };

    constructor(props){
        super(props);
    }

    render(){
        const filteredTypes = this.props.subTypesFor ?
            this.props.plutoProjectTypesList.filter(type=>type.parent===this.props.subTypesFor) :
            this.props.plutoProjectTypesList.filter(type=>!type.hasOwnProperty("parent") || type.parent===null);

        const typesToShow = this.props.onlyShowSubtypes && !this.props.subTypesFor ? [] : filteredTypes;

        return <select id={this.props.id}
                       onChange={event=>this.props.selectionUpdated(parseInt(event.target.value))}
                        value={this.props.selectedType}>
            {typesToShow
                .concat([{id: "",name:"(not set)"}])
                .map(type=><option key={type.id} value={type.id}>{type.name}</option>
            )}
        </select>
    }
}

export default PlutoProjectTypeSelector;