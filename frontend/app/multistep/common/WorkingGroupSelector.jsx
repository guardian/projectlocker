import React from 'react';
import PropTypes from 'prop-types';
import FilterableList from "../../common/FilterableList.jsx";

class WorkingGroupSelector extends React.Component {
    static propTypes = {
        valueWasSet: PropTypes.func.isRequired,
        workingGroupList: PropTypes.array.isRequired,
        currentValue: PropTypes.string.isRequired
    };

    convertContentList(rawContentList) {
        return rawContentList
            .filter(entry=>!entry.hasOwnProperty("hide"))
            .map(entry=>{return {name: entry.name, value: entry.id}})
    }

    render(){
        return <FilterableList onChange={newValue=>this.props.valueWasSet(parseInt(newValue))}
                               value={this.props.currentValue}
                               size={10}
                               unfilteredContent={this.convertContentList(this.props.workingGroupList)}

        />
    }
}

export default WorkingGroupSelector;