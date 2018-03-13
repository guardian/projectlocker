import React from 'react';
import PropTypes from 'prop-types';

class WorkingGroupSelector extends React.Component {
    static propTypes = {
        valueWasSet: PropTypes.func.isRequired,
        workingGroupList: PropTypes.array.isRequired,
        currentValue: PropTypes.string.isRequired
    };


    render(){
        return <select id="working-group-list"
                            onChange={event=>this.props.valueWasSet(parseInt(event.target.value))}
                            defaultValue={this.props.currentValue}>
                {this.props.workingGroupList
                    .filter(wg=>!wg.hasOwnProperty("hide"))
                    .map(wg=><option key={wg.id} value={wg.id}>{wg.name}</option>)
                }
            </select>;
    }
}

export default WorkingGroupSelector;