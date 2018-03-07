import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonCompletionComponent.jsx';
import PostrunActionSelector from "./PostrunActionSelector.jsx";

class DependencyComponent extends CommonMultistepComponent {
    static propTypes = {
        actionsList: PropTypes.array.isRequired,
        selectedDependencies: PropTypes.array.isRequired,
        valueWasSet: PropTypes.func.isRequired,
        currentEntry: PropTypes.number.isRequired
    };

    componentDidUpdate(newProps,newState) {}

    constructor(props){
        super(props);
    }

    render(){
        return <div>
            <h3>Dependencies</h3>
            <p>Please tick any other postrun actions that must complete before this one can execute.</p>
            <PostrunActionSelector actionsList={this.props.actionsList}
                                   valueWasSet={this.props.valueWasSet}
                                   selectedEntries={this.props.selectedDependencies}
                                   shouldExclude={this.props.currentEntry}
            />
        </div>
    }
}

export default DependencyComponent;